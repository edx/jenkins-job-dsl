"""
Assorted helper functions.
"""

AFFIRMATIVE_OPTIONS = ["y", "t", "yes", "true"]


def is_affirmative(string):
    """
    Whether a string corresponds to an affirmative value like "yes" or "True".

    Arguments
        string (string): A string to test against.
    Returns (string):
        True if the input is an affirmative string. False otherwise.
    """
    return string.lower() in AFFIRMATIVE_OPTIONS
