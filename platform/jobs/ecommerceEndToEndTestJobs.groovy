package devops

import hudson.util.Secret
import org.yaml.snakeyaml.Yaml
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_WORKER

/* Sample Secret File
honorCourseId : honor-course-id
verifiedCourseId : verified-course-id
professionalCourseId : professional-course-id
ecommerceUrlRoot : ecommerce-root-url
lmsAutoAuth : true/false
marketingRootUrl : marketing-root-url
lmsUrlRoot : lms-url-root
lmsHttps : true/false
enableCouponAdminTests : true/false
accessToken : access-token
paypalEmail : paypal@email.com
paypalPassword : paypal-password
lmsUsername : lms-username
lmsEmail : lms@email.com
lmsPassword : lms-password
basicAuthUsername : basic-auth-username
basicAuthPassword : basic-auth-password
*/


/* stdout logger */
Map config = [:]
Binding bindings = getBinding()
config.putAll(bindings.getVariables())
PrintStream out = config['out']

/* Map to hold the k:v pairs parsed from the secret file */
Map secretMap = [:]
try {
    out.println('Parsing secret YAML file')
    String secretFileContents = new File("${EDX_ECOMMERCE_END_TO_END_SECRET}").text
    Yaml yaml = new Yaml()
    secretMap = yaml.load(secretFileContents)
    out.println('Successfully parsed secret YAML file')
}
catch (any) {
    out.println('Jenkins DSL: Error parsing secret YAML file')
    out.println('Exiting with error code 1')
    return 1
}

/* Test secret contains all necessary keys for this job */
assert secretMap.containsKey('honorCourseId')
assert secretMap.containsKey('verifiedCourseId')
assert secretMap.containsKey('professionalCourseId')
assert secretMap.containsKey('ecommerceUrlRoot')
assert secretMap.containsKey('lmsAutoAuth')
assert secretMap.containsKey('marketingRootUrl')
assert secretMap.containsKey('lmsUrlRoot')
assert secretMap.containsKey('lmsHttps')
assert secretMap.containsKey('enableCouponAdminTests')
assert secretMap.containsKey('accessToken')
assert secretMap.containsKey('paypalEmail')
assert secretMap.containsKey('paypalPassword')
assert secretMap.containsKey('lmsUsername')
assert secretMap.containsKey('lmsEmail')
assert secretMap.containsKey('lmsPassword')
assert secretMap.containsKey('basicAuthUsername')
assert secretMap.containsKey('basicAuthPassword')

parameters = [
    [
        name: 'HONOR_COURSE_ID',
        description: 'ID of a course with an honor mode in the target environment (e.g., course-v1:org+course+run).',
        default: secretMap['honorCourseId']
    ],
    [
        name: 'VERIFIED_COURSE_ID',
        description: 'ID of a course with a verified mode in the target environment.',
        default: secretMap['verifiedCourseId']
    ],
    [
        name: 'PROFESSIONAL_COURSE_ID',
        description: 'ID of a course with a professional mode in the target environment.',
        default: secretMap['professionalCourseId']
    ],
    [
        name: 'ECOMMERCE_URL_ROOT',
        description: 'URL root for the ecommerce deployment against which to run tests.',
        default: secretMap['ecommerceUrlRoot']
    ],
    [
        name: 'LMS_AUTO_AUTH',
        description: 'Whether to attempt creation of tests users on the target environment.',
        default: secretMap['lmsAutoAuth']
    ],
    [
        name: 'MARKETING_URL_ROOT',
        description: 'URL root for the marketing site against which to run tests.',
        default: secretMap['marketingRootUrl']
    ],
    [
        name: 'LMS_URL_ROOT',
        description: 'URL root for the LMS deployment against which to run tests.',
        default: secretMap['lmsUrlRoot']
    ],
    [
        name: 'LMS_HTTPS',
        description: 'Whether HTTPS is enabled on the LMS deployment against which to run tests.',
        default: secretMap['lmsHttps']
    ],
    [
        name: 'ENABLE_COUPON_ADMIN_TESTS',
        description: 'Whether to run tests against the coupon admin app.',
        default: secretMap['enableCouponAdminTests']
    ],
    [
        name: 'ACCESS_TOKEN',
        description: 'OAuth2 access token used to authenticate requests.',
        default: secretMap['accessToken']
    ],
    [
        name: 'PAYPAL_EMAIL',
        description: 'Email address used to sign into PayPal.',
        default: secretMap['paypalEmail']
    ],
    [
        name: 'PAYPAL_PASSWORD',
        description: 'Password used to sign into PayPal.',
        default: secretMap['paypalPassword']
    ],
    [
        name: 'LMS_USERNAME',
        description: 'Username belonging to an LMS user to use during testing.',
        default: secretMap['lmsUsername']
    ],
    [
        name: 'LMS_EMAIL',
        description: 'Email address belonging to an LMS user to use during testing.',
        default: secretMap['lmsEmail']
    ],
    [
        name: 'LMS_PASSWORD',
        description: 'Password belonging to an LMS user to use during testing.',
        default: secretMap['lmsPassword']
    ],
    [
        name: 'BASIC_AUTH_USERNAME',
        description: 'HTTP Basic auth username for the target environments.',
        default: secretMap['basicAuthUsername']
    ],
    [
        name: 'BASIC_AUTH_PASSWORD',
        description: 'HTTP Basic auth password for the target environments.',
        default: secretMap['basicAuthPassword']
    ],
]

String jobName = 'ecommerce-end-to-end-tests'

job(jobName) {
    description('Job to execute ecommerce IDA end-to-end tests. ' +
                'These tests require access to both the LMS and the E-Commerce service.')

    /* Allow only edx members to see this job */
    authorization {
        blocksInheritance(true)
        permissionAll('edx')
    }

    /* Add the parameters as password parameters, ensure that parameter's default values cannot be seen */
    parameters.each { param ->
        configure {
            it / 'properties' / 'hudson.model.ParametersDefinitionProperty' / parameterDefinitions << 'hudson.model.PasswordParameterDefinition' {
                name param.name
                defaultValue Secret.fromString(param.default.toString()).getEncryptedValue()
                description param.description
            }
        }
    }
    /* Run on jenkins-worker */
    label(JENKINS_PUBLIC_WORKER)
    scm {
        github('edx/ecommerce', '*/master')
    }
    wrappers {
        maskPasswords() //can't see passwords in console
        timestamps()
        buildUserVars()
        colorizeOutput('xterm')
    }
    steps {
        virtualenv {
            name(jobName)
            nature('shell')
                command readFileFromWorkspace('platform/resources/end-to-end-test.sh')
        }
    }
    publishers {
        archiveArtifacts {
            pattern('acceptance_tests/*.xml') // xunit output
            pattern('*.log') // test logs
            pattern('*.png') // test screenshots
        }
        archiveJunit('acceptance_tests/*.xml')
    }
}
