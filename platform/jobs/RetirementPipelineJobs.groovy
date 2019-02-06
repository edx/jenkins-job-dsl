// NOTE: These pipeline-enabled jobs have been retired in favor of the simpler
// jenkins-native jobs in RetirementJobs.groovy which utilize Parameterized
// Trigger plugin and a more python centric approach.

/* UserRetirementPipelineJobs.groovy
 *
 * Defines jobs which orchestrate user retirement.
 */

package platform

import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_TEAM_SECURITY
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_LOG_ROTATOR

// This is the job DSL responsible for creating the main pipeline job.
pipelineJob('User Retirement Pipeline') {
    // NOTE: These pipeline-enabled jobs have been retired in favor of the simpler
    // jenkins-native jobs in RetirementJobs.groovy which utilize Parameterized
    // Trigger plugin and a more python centric approach.
    disabled()

    description('Retire a single user which has confirmed account deletion')

    // Only a subset of edx employees should be allowed to control this job,
    // but customer support can read and discover.
    authorization {
        blocksInheritance(true)
        List membersWithFullControl = ['raccoongang/DevOps']
        membersWithFullControl.each { emp ->
            permissionAll(emp)
        }
        // TODO PLAT-2036: uncomment the following two lines when we add the
        // appropriate github group.
        //permission('hudson.model.Item.Read', 'edx/customer-support')
        //permission('hudson.model.Item.Discover', 'edx/customer-support')
    }

    // Parameters are defined in the pipeline DSL, so leave this line commented
    // out.
    // parameters { }

    definition {
        cps {
            sandbox()  // enable the groovy sandbox
            script(readFileFromWorkspace('platform/resources/RetirementPipelineDefinition.groovy'))
        }
    }
}
