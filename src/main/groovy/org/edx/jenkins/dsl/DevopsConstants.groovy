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
}
