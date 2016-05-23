"""
Test cases for mobile app build utility functions
"""

import ddt
from .. import utils
from unittest import TestCase


@ddt.ddt
class UtilsTestCase(TestCase):
    # pylint: disable=missing-docstring

    @ddt.data("True", "true", "y", "Y", "Yes", "yes", "TRUE", "YES")
    def test_affirmative_yes(self, string):
        """
        Tests that we accept affirmative values
        """
        self.assertTrue(utils.is_affirmative(string))

    @ddt.data(
        "Trueblue", "False", "false", "n",
        "N", "No", "no", "FALSE", "NO", "NONE", "None"
    )
    def test_affirmative_no(self, string):
        """
        Tests that we reject non-affirmative values
        """
        self.assertFalse(utils.is_affirmative(string))
