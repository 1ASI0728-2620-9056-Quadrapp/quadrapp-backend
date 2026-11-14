"""Creates the Quadrapp demo data through the REST API.

The script never touches the database. It signs in with one-time access codes (OTP), reading
them from Mailpit in local development or asking for them on the terminal, and creates, in
order: the institution, its verified email domain, the institution administrator, one parking
lot with zones, levels and spaces, one access, one gateway, one space sensor per space and the
pass sensors of the access. Everything that already exists is reused, so it can run again.

The result (ids and hardware ids) is written as JSON for the MQTT simulator.
See tools/seed/README.md.
"""

from __future__ import annotations

import argparse
import json
import os
import re
import sys
import time
import urllib.parse
from dataclasses import dataclass, field
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Callable

import requests

TOOLS_DIR = Path(__file__).resolve().parent.parent
DEFAULT_OUTPUT = TOOLS_DIR / "out" / "seed-output.json"

OTP_SUBJECT = "Quadrapp - Código de acceso"
INVITATION_SUBJECT = "Quadrapp - Invitación institucional"

# One-time SQL step: the API has no way to create the first platform administrator.
BOOTSTRAP_SQL = (
    "INSERT INTO iam.users (email, role, status, full_name, verified_at)\n"
    "VALUES ('{email}', 'PLATFORM_ADMIN', 'ACTIVE', 'Platform Admin', now())\n"
    "ON CONFLICT (email) DO NOTHING;"
)


class SeedError(Exception):
    """A step failed; the message says what to do."""


class ApiError(SeedError):
    def __init__(self, method: str, url: str, status: int, body: str):
        self.status = status
        super().__init__(f"{method} {url} answered {status}: {body[:500]}")


# ---------------------------------------------------------------------------
# Demo layout
# ---------------------------------------------------------------------------

@dataclass(frozen=True)
class LevelPlan:
    name: str
    label_prefix: str
    sort_order: int


@dataclass(frozen=True)
class ZonePlan:
    name: str
    description: str
    sort_order: int
    levels: tuple[LevelPlan, ...]


# Report decision D4: North, Central and South are zones of one parking lot.
DEMO_ZONES: tuple[ZonePlan, ...] = (
    ZonePlan("Norte", "Ingreso por la avenida principal", 1, (LevelPlan("Nivel 1", "N1", 1),)),
    ZonePlan("Central", "Frente a la biblioteca", 2,
             (LevelPlan("Sótano 1", "C1", 1), LevelPlan("Sótano 2", "C2", 2))),
    ZonePlan("Sur", "Junto a los laboratorios", 3, (LevelPlan("Nivel 1", "S1", 1),)),
)


def space_labels(prefix: str, count: int) -> list[str]:
    """Labels of a level, for example N1-01..N1-08."""
    return [f"{prefix}-{index:02d}" for index in range(1, count + 1)]


def space_position(index: int, per_row: int = 10) -> tuple[float, float]:
    """Grid position in metres (2.5 m wide, 5 m deep spaces)."""
    row, column = divmod(index, per_row)
    return round(column * 2.5, 2), round(row * 5.0, 2)


def extract_otp(text: str) -> str | None:
    match = re.search(r"\b(\d{6})\b", text or "")
    return match.group(1) if match else None


def extract_invitation_token(text: str) -> str | None:
    match = re.search(r"[?&]token=([^\s&]+)", text or "")
    return urllib.parse.unquote(match.group(1)) if match else None


# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------

@dataclass
class Config:
    api_url: str
    mailpit_url: str
    code_source: str
    platform_admin_email: str
    platform_admin_token: str | None
    institution_name: str
    institution_id: int | None
    domain: str
    admin_email: str
    admin_name: str
    lot_name: str
    lot_address: str
    latitude: str
    longitude: str
    spaces_per_level: int
    hardware_prefix: str
    output: Path
    mail_timeout: float = 30.0
    lot_settings: dict[str, Any] = field(default_factory=dict)


def env(name: str, default: str | None = None) -> str | None:
    value = os.environ.get(name)
    return value if value not in (None, "") else default


def parse_args(argv: list[str] | None = None) -> Config:
    parser = argparse.ArgumentParser(description="Create the Quadrapp demo data through the REST API.")
    parser.add_argument("--api-url", default=env("QUADRAPP_API_URL", "http://localhost:8080"))
    parser.add_argument("--mailpit-url", default=env("MAILPIT_URL", "http://127.0.0.1:8025"))
    parser.add_argument("--code-source", choices=("mailpit", "prompt"),
                        default=env("SEED_CODE_SOURCE", "mailpit"),
                        help="Where access codes and invitation links are read from.")
    parser.add_argument("--platform-admin-email",
                        default=env("SEED_PLATFORM_ADMIN_EMAIL", "platform-admin@quadrapp.test"))
    parser.add_argument("--platform-admin-token", default=env("SEED_PLATFORM_ADMIN_TOKEN"),
                        help="Access token of a platform administrator; skips its sign-in.")
    parser.add_argument("--institution-name", default=env("SEED_INSTITUTION_NAME", "Universidad Demo Quadrapp"))
    parser.add_argument("--institution-id", type=int, default=env("SEED_INSTITUTION_ID"),
                        help="Reuse this institution instead of looking it up or creating it.")
    parser.add_argument("--domain", default=env("SEED_DOMAIN", "campus.quadrapp.test"))
    parser.add_argument("--admin-email", default=env("SEED_ADMIN_EMAIL"),
                        help="Institution administrator (default: admin@<domain>).")
    parser.add_argument("--admin-name", default=env("SEED_ADMIN_NAME", "Administrador Demo"))
    parser.add_argument("--lot-name", default=env("SEED_LOT_NAME", "Estacionamiento Monterrico"))
    parser.add_argument("--lot-address", default=env("SEED_LOT_ADDRESS", "Av. Primavera 2390, Santiago de Surco"))
    parser.add_argument("--latitude", default=env("SEED_LATITUDE", "-12.104110"))
    parser.add_argument("--longitude", default=env("SEED_LONGITUDE", "-76.962950"))
    parser.add_argument("--spaces-per-level", type=int, default=int(env("SEED_SPACES_PER_LEVEL", "8")))
    parser.add_argument("--hardware-prefix", default=env("SEED_HARDWARE_PREFIX", "demo"),
                        help="Prefix of every hardware id; hardware ids are unique in the whole platform.")
    parser.add_argument("--output", type=Path, default=Path(env("SEED_OUTPUT", str(DEFAULT_OUTPUT))))
    args = parser.parse_args(argv)

    if not 1 <= args.spaces_per_level <= 99:
        parser.error("--spaces-per-level must be between 1 and 99.")
    if not re.fullmatch(r"[a-z0-9][a-z0-9-]{0,30}", args.hardware_prefix):
        parser.error("--hardware-prefix must be lowercase letters, digits or '-' (max 31).")

    return Config(
        api_url=args.api_url.rstrip("/"),
        mailpit_url=args.mailpit_url.rstrip("/"),
        code_source=args.code_source,
        platform_admin_email=args.platform_admin_email.lower(),
        platform_admin_token=args.platform_admin_token,
        institution_name=args.institution_name,
        institution_id=int(args.institution_id) if args.institution_id else None,
        domain=args.domain.lower(),
        admin_email=(args.admin_email or f"admin@{args.domain}").lower(),
        admin_name=args.admin_name,
        lot_name=args.lot_name,
        lot_address=args.lot_address,
        latitude=args.latitude,
        longitude=args.longitude,
        spaces_per_level=args.spaces_per_level,
        hardware_prefix=args.hardware_prefix,
        output=args.output,
        lot_settings={
            # Inside the ranges of the report: saturation 50-100 %, flow window 5-60 min,
            # minimum detection 3-60 s, accuracy margin 1-30 %.
            "opensAt": "06:00:00",
            "closesAt": "23:00:00",
            "saturationThresholdPct": 85,
            "lowBatteryThresholdPct": 20,
            "staleAfterSeconds": 600,
            "flowWindowMinutes": 15,
            "accuracyMarginPct": 10,
            "occupiedDebounceS": 5,
            "freedDebounceS": 5,
        },
    )


# ---------------------------------------------------------------------------
# HTTP and mail
# ---------------------------------------------------------------------------

class Api:
    def __init__(self, base_url: str, token: str | None = None, session: requests.Session | None = None):
        self.base_url = base_url
        self.token = token
        self.http = session or requests.Session()

    def with_token(self, token: str) -> "Api":
        return Api(self.base_url, token, self.http)

    def request(self, method: str, path: str, body: Any = None, params: dict | None = None,
                allow: tuple[int, ...] = ()) -> requests.Response:
        url = f"{self.base_url}{path}"
        headers = {"Accept": "application/json"}
        if self.token:
            headers["Authorization"] = f"Bearer {self.token}"
        try:
            response = self.http.request(method, url, json=body, params=params, headers=headers, timeout=20)
        except requests.ConnectionError as error:
            raise SeedError(f"Cannot reach the API at {self.base_url}: {error}") from error
        if response.status_code >= 400 and response.status_code not in allow:
            raise ApiError(method, url, response.status_code, response.text)
        return response

    def get(self, path: str, **kwargs) -> Any:
        return self.request("GET", path, **kwargs).json()

    def post(self, path: str, body: Any = None) -> Any:
        response = self.request("POST", path, body=body)
        return response.json() if response.content else None

    def put(self, path: str, body: Any) -> Any:
        return self.request("PUT", path, body=body).json()


class CodeReader:
    """Reads the access code or the invitation link sent to an address."""

    def __init__(self, config: Config, prompt: Callable[[str], str] = input):
        self.config = config
        self.prompt = prompt

    def _mailpit(self, path: str, **params) -> Any:
        try:
            response = requests.get(f"{self.config.mailpit_url}{path}", params=params, timeout=10)
        except requests.ConnectionError as error:
            raise SeedError(f"Cannot reach Mailpit at {self.config.mailpit_url}: {error}. "
                            "Start it or run with --code-source prompt.") from error
        response.raise_for_status()
        return response.json()

    def _message_ids(self, email: str, subject: str) -> list[str]:
        result = self._mailpit("/api/v1/search", query=f'to:"{email}" subject:"{subject}"', limit=50)
        return [message["ID"] for message in result.get("messages", [])]

    def read_after(self, email: str, subject: str, trigger: Callable[[], None],
                   extract: Callable[[str], str | None], what: str) -> str | None:
        """Runs ``trigger`` and returns what ``extract`` finds in the new mail it causes."""
        if self.config.code_source == "prompt":
            trigger()
            answer = self.prompt(f"Paste the {what} sent to {email}: ").strip()
            return extract(answer) or answer or None

        known = set(self._message_ids(email, subject))
        trigger()
        deadline = time.monotonic() + self.config.mail_timeout
        while time.monotonic() < deadline:
            for message_id in self._message_ids(email, subject):
                if message_id not in known:
                    message = self._mailpit(f"/api/v1/message/{message_id}")
                    return extract(message.get("Text", ""))
            time.sleep(0.5)
        return None


def sign_in(api: Api, codes: CodeReader, email: str) -> dict:
    """Signs in with a one-time access code and returns the session resource."""
    code = codes.read_after(
        email, OTP_SUBJECT,
        trigger=lambda: api.post("/api/v1/iam/auth/request-otp", {"email": email}),
        extract=extract_otp, what="6-digit access code",
    )
    if not code:
        raise SeedError(f"No access code arrived for {email}.")
    return api.post("/api/v1/iam/auth/sign-in", {"email": email, "otpCode": code})


# ---------------------------------------------------------------------------
# Seed steps
# ---------------------------------------------------------------------------

def log(message: str) -> None:
    print(message, flush=True)


def find_by(items: list[dict], key: str, value: Any) -> dict | None:
    return next((item for item in items if item.get(key) == value), None)


def platform_admin_session(api: Api, codes: CodeReader, config: Config) -> Api:
    if config.platform_admin_token:
        log("Platform administrator: using the given access token.")
        return api.with_token(config.platform_admin_token)
    try:
        session = sign_in(api, codes, config.platform_admin_email)
    except SeedError as error:
        raise SeedError(
            f"{error}\nThe platform administrator {config.platform_admin_email} probably does not exist. "
            "The API cannot create the first one; run this once against the database and retry:\n\n"
            + BOOTSTRAP_SQL.format(email=config.platform_admin_email)
        ) from error
    if session["user"]["role"] != "PLATFORM_ADMIN":
        raise SeedError(f"{config.platform_admin_email} is {session['user']['role']}, not PLATFORM_ADMIN.")
    log(f"Platform administrator: signed in as {config.platform_admin_email}.")
    return api.with_token(session["accessToken"])


def ensure_institution(platform: Api, config: Config) -> dict:
    if config.institution_id:
        institution = platform.get(f"/api/v1/iam/institutions/{config.institution_id}")
        log(f"Institution: reusing {institution['id']} ({institution['name']}).")
        return institution

    existing_admin = platform.request("GET", "/api/v1/iam/users/by-email",
                                      params={"email": config.admin_email}, allow=(404,))
    if existing_admin.status_code == 200:
        institution = platform.get(f"/api/v1/iam/institutions/{existing_admin.json()['institutionId']}")
        log(f"Institution: reusing {institution['id']} ({institution['name']}), found through {config.admin_email}.")
        return institution

    response = platform.request("POST", "/api/v1/iam/institutions",
                                body={"name": config.institution_name}, allow=(409,))
    if response.status_code == 409:
        raise SeedError(
            f"The institution '{config.institution_name}' already exists, but the API has no way to look it "
            "up by name. Pass its id with --institution-id (or SEED_INSTITUTION_ID)."
        )
    institution = response.json()
    log(f"Institution: created {institution['id']} ({institution['name']}).")
    return institution


def ensure_verified_domain(platform: Api, institution_id: int, domain: str) -> dict:
    domains = platform.get(f"/api/v1/iam/institutions/{institution_id}/domains")
    current = find_by(domains, "domain", domain)
    if current is None:
        current = platform.post(f"/api/v1/iam/institutions/{institution_id}/domains", {"domain": domain})
        log(f"Domain: registered {domain}.")
    if not current["verified"]:
        current = platform.post(f"/api/v1/iam/institutions/domains/{current['id']}/verify")
        log(f"Domain: verified {domain}.")
    else:
        log(f"Domain: {domain} is already verified.")
    return current


def ensure_admin(platform: Api, codes: CodeReader, config: Config, institution_id: int) -> None:
    response = platform.request("GET", "/api/v1/iam/users/by-email",
                                params={"email": config.admin_email}, allow=(404,))
    if response.status_code == 200:
        user = response.json()
        if user["institutionId"] != institution_id or user["role"] != "ADMIN":
            raise SeedError(f"{config.admin_email} is {user['role']} of institution {user['institutionId']}, "
                            f"not ADMIN of {institution_id}. Use another --admin-email.")
        if user["status"] == "ACTIVE":
            log(f"Administrator: {config.admin_email} is already active.")
            return
        if user["status"] != "INVITED":
            raise SeedError(f"{config.admin_email} is {user['status']}; enable it or use another --admin-email.")

    token = codes.read_after(
        config.admin_email, INVITATION_SUBJECT,
        trigger=lambda: platform.post("/api/v1/iam/invitations", {
            "institutionId": institution_id, "email": config.admin_email, "role": "ADMIN"}),
        extract=extract_invitation_token, what="invitation link",
    )
    if not token:
        raise SeedError(f"No invitation arrived for {config.admin_email}.")
    platform.post("/api/v1/iam/invitations/accept", {"invitationToken": token, "fullName": config.admin_name})
    log(f"Administrator: invited and accepted {config.admin_email}.")


def ensure_parking_lot(admin: Api, config: Config) -> dict:
    lot = find_by(admin.get("/api/v1/parking-lots"), "name", config.lot_name)
    if lot:
        log(f"Parking lot: reusing {lot['id']} ({lot['name']}); its settings are kept as they are.")
        return lot
    lot = admin.post("/api/v1/parking-lots", {
        "name": config.lot_name,
        "address": config.lot_address,
        "latitude": float(config.latitude),
        "longitude": float(config.longitude),
        **config.lot_settings,
    })
    log(f"Parking lot: created {lot['id']} ({lot['name']}).")
    return lot


def ensure_child(admin: Api, list_path: str, key: str, value: str, body: dict) -> tuple[dict, bool]:
    existing = find_by(admin.get(list_path), key, value)
    if existing:
        return existing, False
    return admin.post(list_path, body), True


def ensure_layout(admin: Api, config: Config, lot_id: int) -> list[dict]:
    zones_out = []
    created = 0
    for zone_plan in DEMO_ZONES:
        zone, new = ensure_child(admin, f"/api/v1/parking-lots/{lot_id}/zones", "name", zone_plan.name, {
            "name": zone_plan.name, "description": zone_plan.description, "sortOrder": zone_plan.sort_order})
        created += new
        levels_out = []
        for level_plan in zone_plan.levels:
            level, new = ensure_child(admin, f"/api/v1/zones/{zone['id']}/levels", "name", level_plan.name, {
                "name": level_plan.name, "sortOrder": level_plan.sort_order})
            created += new
            existing_spaces = admin.get(f"/api/v1/levels/{level['id']}/spaces")
            spaces_out = []
            for index, label in enumerate(space_labels(level_plan.label_prefix, config.spaces_per_level)):
                space = find_by(existing_spaces, "label", label)
                if space is None:
                    pos_x, pos_y = space_position(index)
                    space = admin.post(f"/api/v1/levels/{level['id']}/spaces", {
                        "label": label,
                        "type": "ACCESSIBLE" if index == 0 else "STANDARD",
                        "posX": pos_x,
                        "posY": pos_y,
                    })
                    created += 1
                spaces_out.append({"id": space["id"], "label": space["label"], "type": space["type"]})
            levels_out.append({"id": level["id"], "name": level["name"], "spaces": spaces_out})
        zones_out.append({"id": zone["id"], "name": zone["name"], "levels": levels_out})
    total = sum(len(level["spaces"]) for zone in zones_out for level in zone["levels"])
    log(f"Layout: {len(zones_out)} zones, {total} spaces ({created} new items).")
    return zones_out


def ensure_access(admin: Api, lot_id: int) -> dict:
    access, new = ensure_child(admin, f"/api/v1/parking-lots/{lot_id}/accesses", "name", "Puerta principal", {
        "name": "Puerta principal", "type": "BIDIRECTIONAL", "flowCountingEnabled": True})
    log(f"Access: {'created' if new else 'reusing'} {access['id']} ({access['name']}).")
    return access


def ensure_device(admin: Api, lot_id: int, hardware_id: str, body: dict) -> tuple[dict, bool]:
    response = admin.request("GET", f"/api/v1/devices/by-hardware/{urllib.parse.quote(hardware_id, safe='')}",
                             allow=(403, 404))
    if response.status_code == 200:
        device = response.json()
        if device["parkingLotId"] != lot_id or device["type"] != body["type"]:
            raise SeedError(f"Hardware id {hardware_id} is already a {device['type']} of parking lot "
                            f"{device['parkingLotId']}. Use another --hardware-prefix.")
        if device["status"] == "DECOMMISSIONED":
            raise SeedError(f"Device {hardware_id} is decommissioned. Use another --hardware-prefix.")
        return device, False
    if response.status_code == 403:
        raise SeedError(f"Hardware id {hardware_id} belongs to another institution. Use another --hardware-prefix.")
    return admin.post("/api/v1/devices", {"parkingLotId": lot_id, "hardwareId": hardware_id, **body}), True


def ensure_devices(admin: Api, config: Config, lot_id: int, zones: list[dict], access: dict) -> dict:
    prefix = config.hardware_prefix
    gateway, new = ensure_device(admin, lot_id, f"{prefix}-gw-01", {
        "type": "GATEWAY", "reportIntervalSeconds": 60, "firmwareVersion": "sim-1.0"})
    log(f"Gateway: {'registered' if new else 'reusing'} {gateway['hardwareId']} ({gateway['id']}).")
    gateway_out = {"id": gateway["id"], "hardwareId": gateway["hardwareId"]}

    space_sensors = []
    created = 0
    for zone in zones:
        for level in zone["levels"]:
            for space in level["spaces"]:
                hardware_id = f"{prefix}-ss-{space['label'].lower()}"
                sensor, new = ensure_device(admin, lot_id, hardware_id, {
                    "type": "SPACE_SENSOR", "reportIntervalSeconds": 300,
                    "gatewayId": gateway["id"], "firmwareVersion": "sim-1.0"})
                created += new
                if sensor.get("gatewayId") != gateway["id"]:
                    raise SeedError(f"Sensor {hardware_id} is linked to gateway {sensor.get('gatewayId')}, "
                                    f"not {gateway['id']}. Use another --hardware-prefix.")
                if sensor.get("spaceId") != space["id"]:
                    sensor = admin.put(f"/api/v1/devices/{sensor['id']}/assignment", {"spaceId": space["id"]})
                space_sensors.append({
                    "id": sensor["id"], "hardwareId": sensor["hardwareId"], "spaceId": space["id"],
                    "spaceLabel": space["label"], "zone": zone["name"], "level": level["name"],
                    "gatewayHardwareId": gateway["hardwareId"],
                })
    log(f"Space sensors: {len(space_sensors)} linked to {gateway['hardwareId']} ({created} new).")

    pass_sensors = []
    for lane in (1, 2):
        hardware_id = f"{prefix}-ps-{lane:02d}"
        sensor, new = ensure_device(admin, lot_id, hardware_id, {
            "type": "PASS_SENSOR", "reportIntervalSeconds": 300, "gatewayId": gateway["id"],
            "directional": True, "firmwareVersion": "sim-1.0"})
        if sensor.get("accessId") != access["id"]:
            sensor = admin.put(f"/api/v1/devices/{sensor['id']}/assignment", {"accessId": access["id"]})
        pass_sensors.append({"id": sensor["id"], "hardwareId": sensor["hardwareId"], "accessId": access["id"],
                             "gatewayHardwareId": gateway["hardwareId"]})
    log(f"Pass sensors: {', '.join(sensor['hardwareId'] for sensor in pass_sensors)} on {access['name']}.")
    return {"gateways": [gateway_out], "spaceSensors": space_sensors, "passSensors": pass_sensors}


def run(config: Config, api: Api | None = None, codes: CodeReader | None = None) -> dict:
    api = api or Api(config.api_url)
    codes = codes or CodeReader(config)

    platform = platform_admin_session(api, codes, config)
    institution = ensure_institution(platform, config)
    domain = ensure_verified_domain(platform, institution["id"], config.domain)
    ensure_admin(platform, codes, config, institution["id"])

    session = sign_in(api, codes, config.admin_email)
    admin = api.with_token(session["accessToken"])
    log(f"Administrator: signed in as {config.admin_email}.")

    lot = ensure_parking_lot(admin, config)
    zones = ensure_layout(admin, config, lot["id"])
    access = ensure_access(admin, lot["id"])
    devices = ensure_devices(admin, config, lot["id"], zones, access)

    output = {
        "generatedAt": datetime.now(timezone.utc).isoformat(timespec="seconds").replace("+00:00", "Z"),
        "apiUrl": config.api_url,
        "tenantId": institution["id"],
        "institution": {"id": institution["id"], "name": institution["name"], "domain": domain["domain"]},
        "admin": {"id": session["user"]["id"], "email": config.admin_email},
        "parkingLot": {key: lot[key] for key in (
            "id", "name", "latitude", "longitude", "saturationThresholdPct", "flowWindowMinutes",
            "accuracyMarginPct", "occupiedDebounceS", "freedDebounceS", "staleAfterSeconds")},
        "zones": zones,
        "accesses": [{"id": access["id"], "name": access["name"], "type": access["type"]}],
        **devices,
    }
    config.output.parent.mkdir(parents=True, exist_ok=True)
    config.output.write_text(json.dumps(output, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    log(f"Seed output written to {config.output}.")
    return output


def main(argv: list[str] | None = None) -> int:
    config = parse_args(argv)
    try:
        run(config)
    except SeedError as error:
        print(f"Seed failed: {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
