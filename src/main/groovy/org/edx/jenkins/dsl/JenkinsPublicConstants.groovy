package org.edx.jenkins.dsl

class JenkinsPublicConstants {

    public static final Closure GHPRB_CANCEL_BUILDS_ON_UPDATE(boolean override) {
        return {
            it / 'triggers' / 'org.jenkinsci.plugins.ghprb.GhprbTrigger' / 'extensions' / 'org.jenkinsci.plugins.ghprb.extensions.build.GhprbCancelBuildsOnUpdate' {
                overrideGlobal(override)
            }
        }
    }

    public static final Closure DEFAULT_VIEW = {
        return {
            status()
            weather()
            name()
            lastSuccess()
            lastFailure()
            lastDuration()
            buildButton()
        }
    }
}
