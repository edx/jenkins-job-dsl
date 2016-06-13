package org.edx.jenkins.dsl

import org.yaml.snakeyaml.Yaml

class JenkinsPublicConstants {
  
  public static final String JENKINS_PUBLIC_BASE_URL = "https://build.testeng.edx.org/"

  public static final Closure JENKINS_PUBLIC_LOG_ROTATOR = {
    return {
      daysToKeep(14)
      numToKeep(-1)
      artifactDaysToKeep(-1)
      artifactNumToKeep(-1)
    }
  }

  public static final String JENKINS_PUBLIC_WORKER = "jenkins-worker"

  public static final Closure JENKINS_PUBLIC_ARCHIVE_ARTIFACTS = {
    return {
        pattern("reports/**/*,test_root/log/**/*.png,test_root/log/**/*.log, test_root/log/**/hars/*.har,**/nosetests.xml," +
                "**/TEST-*.xml"
                )
        exclude("reports/bok-choy/*/bok_choy/cover/*")
        allowEmpty(true)
        defaultExcludes(true)
    }
  }

  public static final Closure JENKINS_PUBLIC_ARCHIVE_XUNIT = {
    return {
        jUnit {
            pattern("**/nosetests.xml,**/TEST-*.xml,reports/acceptance/*.xml,reports/quality.xml," +
                    "reports/javascript/javascript_xunit.xml,reports/bok_choy/xunit.xml,reports/bok_choy/**/xunit.xml"
                    )
            skipNoTestFiles(true)
            stopProcessingIfError(true)
            failIfNotNew(false)
            deleteOutputFiles(false)
        }
        failedThresholds {
            failure()
            failureNew()
            unstable()
            unstableNew()
        }
        skippedThresholds {
            failure()
            failureNew()
            unstable()
            unstableNew()
        }
    }
  }

  public static final Closure JENKINS_PUBLIC_HIPCHAT = {authToken ->
    return {
            token(authToken)
            rooms('new-jenkins-chatter')
            notifyAborted()
            notifyFailure()
            notifyUnstable()
            notifyBackToNormal()
            startJobMessage('$JOB_NAME #$BUILD_NUMBER $STATUS ($CHANGES_OR_CAUSE) (<a href="$URL">Open</a>)')
            completeJobMessage('$JOB_NAME #$BUILD_NUMBER $STATUS after $DURATION (<a href="$URL">Open</a>)')
    }
  }

    /* Parse data out of the Jenkins secret file referenced with env var "secretFileVariable" */
    /* Secret files are in YAML format, so parse their k:v into a a Map */
    public static final Closure JENKINS_PUBLIC_PARSE_SECRET = { secretFileVariable, envVarsMap, out ->
        def secretMap = [:]
        String fileContents = new File(envVarsMap[secretFileVariable]).text
        Yaml yaml = new Yaml()
        secretMap = yaml.load(fileContents)
        return secretMap
    }
    
    public static final String JENKINS_PUBLIC_JUNIT_REPORTS = 'edx-platform/**/nosetests.xml,edx-platform/reports/acceptance/*.xml,' +
                                                              'edx-platform/reports/quality.xml,edx-platform/reports/javascript/' +
                                                              'javascript_xunit.xml,edx-platform/reports/bok_choy/xunit.xml,edx-platform/' +
                                                              'reports/bok_choy/**/xunit.xml'

    public static final Closure JENKINS_PUBLIC_GITHUB_STATUS_PENDING = { predefinedPropsMap ->
        return {
            trigger('github-build-status') {
                parameters {
                    predefinedProps(predefinedPropsMap)
                    predefinedProp('BUILD_STATUS', 'pending')
                    predefinedProp('DESCRIPTION', 'Pending')
                }
            }
        }
    }

    public static final Closure JENKINS_PUBLIC_GITHUB_STATUS_SUCCESS = { predefinedPropsMap ->
        return {
            trigger('github-build-status') {
                condition('SUCCESS')
                parameters {
                    predefinedProps(predefinedPropsMap)
                    predefinedProp('BUILD_STATUS', 'success')
                    predefinedProp('DESCRIPTION', 'Build Passed')
                    predefinedProp('CREATE_DEPLOYMENT', 'true')
                }
            }
        }
    }

    public static final Closure JENKINS_PUBLIC_GITHUB_STATUS_UNSTABLE_OR_WORSE = { predefinedPropsMap ->
        return {
            trigger('github-build-status') {
                condition('UNSTABLE_OR_WORSE')
                parameters {
                    predefinedProps(predefinedPropsMap)
                    predefinedProp('BUILD_STATUS', 'failure')
                    predefinedProp('DESCRIPTION', 'Build Failed')
                }
            }
        }
    }
}
