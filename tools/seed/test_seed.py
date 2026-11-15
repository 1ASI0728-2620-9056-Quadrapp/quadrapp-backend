import json
import unittest

import requests

import seed


class FakeApi(seed.Api):
    """Answers each (method, path) with a canned status and JSON body and records every call."""

    def __init__(self, routes: dict):
        super().__init__("http://api.test")
        self.routes = routes
        self.calls = []

    def request(self, method, path, body=None, params=None, allow=()):
        self.calls.append((method, path, body, params))
        status, payload = self.routes[(method, path)]
        response = requests.Response()
        response.status_code = status
        response._content = json.dumps(payload).encode()
        if status >= 400 and status not in allow:
            raise seed.ApiError(method, path, status, response.text)
        return response


class ResourceEndpointsTest(unittest.TestCase):
    def test_verifies_an_unverified_domain_with_a_partial_update_of_the_domain(self):
        api = FakeApi({
            ("GET", "/api/v1/iam/institutions/7/domains"): (200, [{"id": 3, "domain": "upc.edu.pe", "verified": False}]),
            ("PATCH", "/api/v1/iam/institutions/7/domains/3"): (200, {"id": 3, "domain": "upc.edu.pe", "verified": True}),
        })

        domain = seed.ensure_verified_domain(api, 7, "upc.edu.pe")

        self.assertTrue(domain["verified"])
        self.assertIn(("PATCH", "/api/v1/iam/institutions/7/domains/3", {"status": "VERIFIED"}, None), api.calls)

    def test_finds_a_device_by_filtering_the_devices_collection_by_hardware_id(self):
        device = {"id": 9, "parkingLotId": 4, "type": "GATEWAY", "status": "ACTIVE"}
        api = FakeApi({("GET", "/api/v1/devices"): (200, [device])})

        found, created = seed.ensure_device(api, 4, "GW 01", {"type": "GATEWAY"})

        self.assertEqual(found, device)
        self.assertFalse(created)
        self.assertEqual(api.calls, [("GET", "/api/v1/devices", None, {"hardwareId": "GW 01"})])

    def test_registers_the_device_when_the_filter_finds_none(self):
        created_device = {"id": 10, "parkingLotId": 4, "type": "GATEWAY", "status": "ACTIVE"}
        api = FakeApi({
            ("GET", "/api/v1/devices"): (200, []),
            ("POST", "/api/v1/devices"): (201, created_device),
        })

        found, created = seed.ensure_device(api, 4, "GW-01", {"type": "GATEWAY"})

        self.assertEqual(found, created_device)
        self.assertTrue(created)


class MailParsingTest(unittest.TestCase):
    def test_extracts_the_access_code_from_the_otp_mail(self):
        text = "Tu código de acceso es: 048213\nCaduca pronto. Si no lo solicitaste, ignora este correo."
        self.assertEqual(seed.extract_otp(text), "048213")

    def test_returns_none_when_the_mail_has_no_code(self):
        self.assertIsNone(seed.extract_otp("Sin código"))

    def test_extracts_and_decodes_the_invitation_token(self):
        text = ("Has recibido una invitación para Quadrapp. Acepta aquí: "
                "http://localhost:4200/accept-invitation?token=abc%2Bdef%3D")
        self.assertEqual(seed.extract_invitation_token(text), "abc+def=")


class LayoutTest(unittest.TestCase):
    def test_labels_are_numbered_per_level(self):
        self.assertEqual(seed.space_labels("N1", 3), ["N1-01", "N1-02", "N1-03"])

    def test_positions_fill_rows_of_ten(self):
        self.assertEqual(seed.space_position(0), (0.0, 0.0))
        self.assertEqual(seed.space_position(11), (2.5, 5.0))

    def test_demo_zones_follow_decision_d4(self):
        self.assertEqual([zone.name for zone in seed.DEMO_ZONES], ["Norte", "Central", "Sur"])


if __name__ == "__main__":
    unittest.main()
