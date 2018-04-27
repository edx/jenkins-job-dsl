# jenkins-job-dsl

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

## Running a local Jenkins for development

See [README-Hacking](README-Hacking.md) for details on how to spin up a local docker container with
an environment similar to one you would find on the TestEng "Build Jenkins" or DevOps "Tools Jenkins".

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

### Example Job
An example job can be found [here](platform/jobs/exampleJob.groovy). 

### Credentials and Secrets

Several techniques for managing credentials and secrets are discussed here

https://openedx.atlassian.net/wiki/display/TE/Jenkins+secrets+management+with+Job+DSL

### Shared Constants:

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
