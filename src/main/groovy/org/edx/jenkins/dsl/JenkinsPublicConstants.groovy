package org.edx.jenkins.dsl

import org.yaml.snakeyaml.Yaml
import hudson.util.Secret

class JenkinsPublicConstants {

  public static final String JENKINS_PUBLIC_BASE_URL = "https://build.testeng.edx.org/"

  public static final Closure JENKINS_PUBLIC_LOG_ROTATOR = {
    int days=14, int num=-1, int artifactDays=-1, int artifactNum=-1 ->
    return {
      daysToKeep(days)
      numToKeep(num)
      artifactDaysToKeep(artifactDays)
      artifactNumToKeep(artifactNum)
    }
  }

  public static final String JENKINS_PUBLIC_GITHUB_BASEURL = "https://github.com/"

  public static final String JENKINS_PUBLIC_WORKER = "jenkins-worker"

  public static final Closure JENKINS_PUBLIC_ARCHIVE_ARTIFACTS = {
    return {
        pattern("reports/**/*,test_root/log/**/*.png,test_root/log/**/*.log, test_root/log/**/hars/*.har,**/nosetests.xml," +
                "**/TEST-*.xml,*.log"
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
                    "reports/javascript/javascript_xunit.xml,reports/a11y/xunit.xml,reports/bok_choy/xunit.xml,reports/bok_choy/**/xunit.xml"
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

    public static final String JENKINS_PUBLIC_JUNIT_REPORTS = 'edx-platform*/**/nosetests.xml,edx-platform*/reports/acceptance/*.xml,' +
                                                              'edx-platform*/reports/quality.xml,edx-platform*/reports/javascript/' +
                                                              'javascript_xunit*.xml,edx-platform*/reports/a11y/**/xunit.xml,' +
                                                              'edx-platform*/reports/bok_choy/**/xunit.xml'

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

    public static final Closure JENKINS_PUBLIC_TEAM_SECURITY = { memberList ->

        return {
            blocksInheritance(true)
            // grant team members control of job
            memberList.each { member ->
                permissionAll(member)
            }
            // grant read/run rights to the org
            permission('hudson.model.Item.Read', 'edx')
            permission('hudson.model.Item.Discover', 'edx')
            permission('hudson.model.Item.Build', 'edx')
            permission('hudson.model.Item.Cancel', 'edx')
        }

    }

    public static final Closure GENERAL_PRIVATE_JOB_SECURITY = {
        return {
            blocksInheritance(true)
            permissionAll('edx')
            permission('hudson.model.Item.Discover', 'anonymous')
        }
    }

    public static final Closure PUBLISH_TO_HOCKEY_APP(String hockeyAppApiToken, String apkFilePath, String releaseNotesString) {
        return {
            it /
                publishers /
                'org.jenkins__ci.plugins.flexible__publish.FlexiblePublisher' /
                publishers /
                'org.jenkins__ci.plugins.flexible__publish.ConditionalPublisher' /
                publisherList /
                'hockeyapp.HockeyappRecorder' (schemaVersion: '2') {
                    applications {
                        'hockeyapp.HockeyappApplication' (plugin: 'hockeyapp@1.2.1', schemaVersion: '1') {
                            apiToken hockeyAppApiToken
                            notifyTeam true
                            filePath apkFilePath
                            downloadAllowed true
                            releaseNotesMethod (class: "net.hockeyapp.jenkins.releaseNotes.ManualReleaseNotes") {
                                releaseNotes releaseNoteString
                                isMarkDown false
                            }
                            uploadMethod (class: "net.hockeyapp.jenkins.uploadMethod.AppCreation") {}
                        }
                    }
                }
        }
    }

    public static final Closure GHPRB_WHITELIST_BRANCH(String branchRegex) {
        return {
            it / triggers / 'org.jenkinsci.plugins.ghprb.GhprbTrigger' << whiteListTargetBranches {
                'org.jenkinsci.plugins.ghprb.GhprbBranch' {
                    branch branchRegex
                }
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

    // Reusable closure for configuring a masked password user parameter with a default value
    // Input must be structured in the following format:
    // [ name: String x, description: String y, default: String z ]
    public static final Closure JENKINS_PUBLIC_MASKED_PASSWORD = { param ->
        return {
            it / 'properties' / 'hudson.model.ParametersDefinitionProperty' / parameterDefinitions << 'hudson.model.PasswordParameterDefinition' {
                name param.name
                defaultValue Secret.fromString(param.default.toString()).getEncryptedValue()
                description param.description
            }
        }

    }

}
