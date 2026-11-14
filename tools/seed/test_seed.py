import unittest

import seed


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
