[![CI](https://github.com/edx/jenkins-job-dsl/actions/workflows/ci.yaml/badge.svg)](https://github.com/edx/jenkins-job-dsl/actions/workflows/ci.yaml)

# jenkins-job-dsl

This repository conatins Jenkins Job DSL code for generating Jenkins jobs on several edx.org Jenkins servers including
tools-edx-jenkins. It has a companion private repo for generating the Jobs on the edx.org
Jenkins servers. Usage of this code without that repo may be difficult.

## Code Status

This code is in active development.

## Contributing

While this repo is open source in the hopes that it would be useful, it's heavily entanged with a private repo to the
point where, practically it doesn't work without the private repo. While open to PRs, it's impractical for anyone
outside edx to use it.

## Getting Started

### Running a local Jenkins for development
See [README-Hacking](README-Hacking.md) for details on how to spin up a local docker container with
an environment similar to one you would find on the DevOps "Tools Jenkins".

## File structure

    .
    ├── <project>                       # Example project name e.g. mobile app
    │   ├── jobs                        # Job DSL code
    │   ├── resources                   # Resources specific to this project
    ├── <other_project>                 # Another project name e.g. platform
    │   ├── jobs                        # Job DSL code
    │   ├── resources                   # Resources specific to this project
    ├── <...>                           # A folder for every project!
    ├── src                             # Code shared between each project
    │   ├── main
    │   │   ├── groovy                  # support classes
    │   │   └── resources
    │   │       └── idea.gdsl           # IDE support for IDEA
    │   └── test
    │       └── groovy                  # specs (organized by project)
    │           └──<project>
    │           └──<other_project>
    └── build.gradle                    # build file

## Testing

### Spec Testing
DSL jobs can be tested using the [Spock test framework](http://spockframework.github.io/spock/docs/1.0/index.html).

To run all of the test specs:
`./gradlew test`
To run all of the test specs within a given project:
`./gradlew test --tests sample.*`
To run all a single test spec:
`./gradlew test --tests sample.SampleJobSpec`

### Static Analysis
You can run codenarc to verify well written Groovy code.

To run codenarc on the dsl jobs:
`./gradlew codenarcJobs`
To run codenarc on the shared code:
`./gradlew codenarcSrc`
etc...

## Writing DSLs

### Disabling a Job

If a job is causing problems and you want to disable it and add the reason to the
job description you can use code like the following.

```groovy
    return dslFactory.job(extraVars.get("FOLDER_NAME") + "/${environment}-${deployment}-${jobName}") {
        ...
        // Disabled until https://example.atlassian.net/browse/EX-123 is resolved
        disabled()
        description('Disabled until <a href="https://example.atlassian.net/browse/EX-123">EX-123</a> is resolved.')
        ...
```

### Example Job
An example job can be found [here](sample/jobs/sampleJob.groovy).

### Credentials and Secrets

Several techniques for managing credentials and secrets are discussed here

https://openedx.atlassian.net/wiki/display/TE/Jenkins+secrets+management+with+Job+DSL

### Shared Constants

In addition to the yaml parsing techniques in the wiki link above, you can also manage
shared constants directly in your DSL script.
* Use a try/catch to parse the Yaml file into a map
```groovy
    Map constantsMap = [:]
    try {
        out.println('Parsing secret YAML file')
        String constantsConfig = new File("${EDX_PLATFORM_SHARED_CONSTANTS}").text
        Yaml yaml = new Yaml()
        constantsMap = yaml.load(constantsConfig)
        out.println('Successfully parsed secret YAML file')
    }
    catch (any) {
        out.println('Jenkins DSL: Error parsing secret YAML file')
        out.println('Exiting with error code 1')
        return 1
    }
```
* Assert that the map contains the desired constants
* Use the constant as the key value in the map ex. constantsMap['credential']

Constant | Purpose | Use
------------ | ------------- | -------------
credential | Allow access to private git repositories | In the credential() function in the Git Scm Context

### Structure

TL;DR Making a new job? Make it look like [this one](https://github.com/edx/jenkins-job-dsl/blob/312355c0568328b3d7bacbb00c2e94a6f30f01eb/testeng/jobs/backupJenkins.groovy#L47) NOT like [this one](https://github.com/edx/jenkins-job-dsl/blob/master/dataeng/jobs/analytics/AggregateDailyTrackingLogs.groovy#L11).

There are two common ways to write jobs:
1: The Build jenkins jobs are often flattened (as shown in [backupJenkins.groovy](https://github.com/edx/jenkins-job-dsl/blob/312355c0568328b3d7bacbb00c2e94a6f30f01eb/testeng/jobs/backupJenkins.groovy#L47) ) or if there are many jobs that are similar you can loop over a list of Maps like we do in [upgradePythonRequirements.groovy](https://github.com/edx/jenkins-job-dsl/blob/312355c0568328b3d7bacbb00c2e94a6f30f01eb/testeng/jobs/upgradePythonRequirements.groovy#L206). This has the benefit of keeping all the logic in one file and being easy to understand.
2: DevOps and Data Engineering jenkins jobs have often wrapped jobs in classes (as shown in [AggregateDailyTrackingLogs.groovy](https://github.com/edx/jenkins-job-dsl/blob/master/dataeng/jobs/analytics/AggregateDailyTrackingLogs.groovy#L11)). This paradigm has the benefit of allowing you to import the class in multiple places to be DRYer. There are also a few places where we link configuration to static functions from classes like we do in [createJobs.groovy](https://github.com/edx/jenkins-job-dsl/blob/master/dataeng/jobs/createJobs.groovy).

In general we would like to transition away from using classes in order to make it easier for people to reason about our code. To that end, please use the first methodology when possible.

### Gotchas

1: As of this writing Tools Jenkins has not been upgraded to Jenkins 2 and does not support pipelines.
2: As of this writing our Data Engineering instance defines credentials globally and does not use seed jobs that deploy jobs into separate folders with separate credentials.
3: We want to reduce the number of git checkouts so that our jobs can still run when GitHub is down, especially for jobs that are triggered frequently.

