"""
Exceptions used by the mobile app build scripts
"""

# Trigger Build


class MissingEnvironmentVariable(Exception):
    """
    Indicates that an expected environment variable was not found.
    """
    def __init__(self, variable):
        self.variable = variable
        super(MissingEnvironmentVariable, self).__init__()

    def __str__(self):
        return "Missing environment variable: {variable}".format(
            variable=self.variable
        )


class BranchAlreadyExists(Exception):
    """
    Indicates that the branch we're trying to create already exists
    """
    def __init__(self, branch_name):
        self.branch_name = branch_name
        super(BranchAlreadyExists, self).__init__()

    def __str__(self):
        return "Branch already exists: {branch}".format(
            branch=self.branch_name
        )


# Upload build


class UploadFailure(Exception):
    """
    Indicates that a build failed to upload
    """
    def __init__(self, response):
        self.response = response
        super(UploadFailure, self).__init__()

    def __str__(self):
        return "Upload Failed. HTTP {code}. data:{data}".format(
            code=self.response.status_code,
            data=self.response.text
        )
