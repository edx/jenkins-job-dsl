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

    // Checks out the 3 common multiscm repos
    public static def common_configuration_multiscm = { extraVars ->
        def gitCredentialId = extraVars.get('SECURE_GIT_CREDENTIALS','')
        return {
            git {
                remote {
                    url('$CONFIGURATION_REPO')
                    branch('$CONFIGURATION_BRANCH')
                    relativeTargetDir('configuration')
                }
                clean(true)
                pruneBranches(true)
            }
            git {
                remote {
                    url('$CONFIGURATION_INTERNAL_REPO')
                    branch('$CONFIGURATION_INTERNAL_BRANCH')
                    relativeTargetDir('configuration-internal')
                    if (gitCredentialId) {
                        credentials(gitCredentialId)
                    }
                }
                clean(true)
                pruneBranches(true)
            }
            git {
                remote {
                    url('$CONFIGURATION_SECURE_REPO')
                    branch('$CONFIGURATION_SECURE_BRANCH')
                    relativeTargetDir('configuration-secure')
                    if (gitCredentialId) {
                        credentials(gitCredentialId)
                    }
                }
                clean(true)
                pruneBranches(true)
            }
        }
    }
}
