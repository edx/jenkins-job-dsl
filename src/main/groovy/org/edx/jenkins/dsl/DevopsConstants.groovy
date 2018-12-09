package org.edx.jenkins.dsl

class DevopsConstants {

    public static def common_wrappers = {
        timestamps()
        buildUserVars()
        maskPasswords()
    }

     public static def common_logrotator = {
         daysToKeep(7)
     }

    public static final common_read_permissions = [
        'hudson.model.Item.Build',
        'hudson.model.Item.Cancel',
        'hudson.model.Item.Discover',
        'hudson.model.Item.Read',
    ]
       
    // Standard 3 repo checkout used by many ansible tasks
    public static def common_configuration_parameters = { extraVars ->
        return {
            stringParam('CONFIGURATION_REPO', extraVars.get('CONFIGURATION_REPO', 'https://github.com/edx/configuration.git'),
                    'Git repo containing edX configuration.')
            stringParam('CONFIGURATION_BRANCH', extraVars.get('CONFIGURATION_BRANCH', 'master'),
                    'e.g. tagname or origin/branchname')
            stringParam('CONFIGURATION_INTERNAL_REPO', extraVars.get('CONFIGURATION_INTERNAL_REPO'),
                    'Git repo containing internal overrides')
            stringParam('CONFIGURATION_INTERNAL_BRANCH', extraVars.get('CONFIGURATION_INTERNAL_BRANCH', 'master'),
                    'e.g. tagname or origin/branchname')
            stringParam('CONFIGURATION_SECURE_REPO', extraVars.get('CONFIGURATION_SECURE_REPO'),
                    'Git repo containing secure overrides')
            stringParam('CONFIGURATION_SECURE_BRANCH', extraVars.get('CONFIGURATION_SECURE_BRANCH', 'master'),
                    'e.g. tagname or origin/branchname')
        }
    }

    // Checks out the 3 common multiscm repos
    public static def common_configuration_multiscm = { extraVars ->
        def gitCredentialId = extraVars.get('SECURE_GIT_CREDENTIALS','')
        return {
            git {
                remote {
                    url('$CONFIGURATION_REPO')
                    branch('$CONFIGURATION_BRANCH')
                }
                extensions {
                    cleanAfterCheckout()
                    pruneBranches()
                    relativeTargetDirectory('configuration')
                }
            }
            git {
                remote {
                    url('$CONFIGURATION_INTERNAL_REPO')
                    branch('$CONFIGURATION_INTERNAL_BRANCH')
                    if (gitCredentialId) {
                        credentials(gitCredentialId)
                    }
                }
                extensions {
                    cleanAfterCheckout()
                    pruneBranches()
                    relativeTargetDirectory('configuration-internal')
                }
            }
            git {
                remote {
                    url('$CONFIGURATION_SECURE_REPO')
                    branch('$CONFIGURATION_SECURE_BRANCH')
                    if (gitCredentialId) {
                        credentials(gitCredentialId)
                    }
                }
                extensions {
                    cleanAfterCheckout()
                    pruneBranches()
                    relativeTargetDirectory('configuration-secure')
                }
            }
        }
    }

    public static def merge_to_master_trigger = { branchName ->
        return { 
            // due to a bug or misconfiguration, jobs with default branches with
            // slashes are indiscriminately triggered by pushes to other branches.
            // For more information, see:
            // https://openedx.atlassian.net/browse/TE-1921
            // for commits merging into master, trigger jobs via github pushes
            if (branchName == 'master') {
                githubPush()
            }
        }
    }
}
