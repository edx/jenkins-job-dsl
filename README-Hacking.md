# Local Development

The simplest way to get started with local development edx-related jenkins services is to use our Docker containers.
These containers are pre-configured to run Jenkins. The `docker-compose.yml` file in the root of this repository
contains the configuration necessary to bring a containers online with the proper ports exposed, and volumes shared.

Set up a local development environment by entering a Python 3 virtualenv
at the root of the repository and running `make requirements`
(installs docker-compose). Then start a service:

    $ make docker.run.<name_of_jenkins_service>

Where `<name_of_jenkins_service>` corresponds to services specified in docker-compose.yml (e.g. `jenkins_tools`, `jenkins_build`, etc.).

A volume will be created for the container, which contains all of the configuration for Jenkins and will persist between
container stops and starts. For the `jenkins_tools` container, however, plugin updates or installs will cause the
volume to become recreated (see WIP: Updating the Docker Image below).

Once the container is running, you can connect to Jenkins at `http://localhost:<port>`. Ports are also configured in
`docker-compose.yml`. (8080 for jenkins_tools, 8081 for jenkins_build)

In order to bootstrap Jenkins in a container you will need to do two things:

1. Set up any credentials required for pulling private repositories.
2. Set up a seed job to process the job DSL (this job is automatically created in jenkins_build).

### Credentials

You will need credentials--SSH key, or username and password--to clone private Git repositories. If you are using
 GitHub, use a scoped [personal access token](https://github.com/settings/tokens) to limit potential security risks. A 'repo' scope should be sufficient for most jobs.

Credentials can be added using the Jenkins UI
You create a username/password combination where the password is your token, this is required if you are using MFA but still want to use a password

Note that if using a personal access token, you can only clone https://github.com/ URLs, and ssh keys only work on git@github.com:edx
URLs. We use git@github.com URLs on tools-edx-jenkins with deployment keys, but you can use whatever is simpler in testing.

### Creating the Seed Job

You will need to create a new Jenkins job that executes the DSL, and creates other jobs. This is referred to as the
"seed job". Note that these instructions are for a simple DSL, your seed job documentation may specify cloning multiple
DSL/configuration repos, or running Gradle.

1. Use the Jenkins UI to create a new **Freestyle project** job named **Job Creator**.
2. Configure the job to use **Multiple SCMs** for *Source Control Management*, and add a Git repository. (Note that we
are NOT using the Git plugin here.)
    1. Set the Repository URL to the repo containing your job DSL (e.g. git@github.com:edx/jenkins-job-dsl-internal.git). (At the time of this writing there is a top level groovy script for each seed job in dsl-internal/jobs.)
    2. If necessary, select your authentication credentials (e.g. SSH key or the personal token from above).
    3. Repeat for jenkins-job-dsl and edx-internal (and possibly edge-internal).
    4. For each repository add an additional behaviour and check them out into a subdirectory of the same name i.e. jenkins-job-dsl-internal
       EXCEPT for jenkins-job-dsl, do not check this out into a subdirectory.
4. Add a **Invoke Gradle script** build step
    1. Select Use Graddle Wrapper
    1. check 'Make gradlew executable'
    2. check 'From Root Build Script Dir'
    3. tasks: 'clean libs'
5. Add a **Process Job DSLs** build step and configure it using the settings below. Remember to click the *Advanced*
button to expose the final fields.
    1. DSL Scripts: The path to your seed job DSL script.
       For example, `jenkins-job-dsl-internal/jobs/tools-edx-jenkins.edx.org/createSandboxJobs.groovy`
       if you wanted to create an entry for sandbox jobs.
    2. Action for existing jobs and views: Unchecked
    3. Action for removed jobs: Delete
    4. Action for removed views: Ignore
    5. Context to use for relative job names: Jenkins Root
    6. Additional classpath: Click on the down-arrow to get a text box and enter the classpath specified by your seed job.
    7. Fail build if a plugin must be installed or update: checked
    8. Mark build as unstable when using deprecated features: checked
6. Look at the docstring of seed job script that you referenced in the previous step,
   and follow any additional steps it specifies.
   For example, `createSandboxJobs.groovy` requires you to modify the **Additional Classpath**
   setting.
7. Save the job, and Build it with Parameters.

### Common Problems

1. How to fix: Path issues

    workspace:/jenkins-job-dsl-internal/jobs/tools-edx-jenkins.edx.org/createMonitoringJobs.groovy: 180: unable to resolve class org.yaml.snakeyaml.error.YAMLException
     @ line 180, column 1.
       import org.yaml.snakeyaml.error.YAMLException
       ^

Under advanced (On the far right of the UI) for 'Process Job DSLs' you need to add the valid groovy class path that is required to locate your groovy files. It is new line delimited, and might look like the following:

    jenkins-job-dsl-internal/src/main/groovy/
    jenkins-job-dsl-internal/lib/*.jar
    src/main/groovy/
    lib/*.jar
    .

2. Null pointer on extra_vars

    java.lang.NullPointerException
    ......
    at createMonitoringJobs.run(createMonitoringJobs.groovy:208)


On a line like so:

    UpdateCeleryMonitoring(this, globals + extraVars.get('UPDATE_CELERY_MONITORING_VARS'))


You need to change the build into a parameterized build
and set a text parameter for EXTRA_VARS with the value:

    @edx-internal/tools-edx-jenkins/monitoring.yml

There exists one of these for most seeders

3. Unknown parent path

An example error might look like:

    Processing DSL script createMonitoringJobs.groovy
    ERROR: Could not create item, unknown parent path in "Monitoring/check-redis-loadtest-edx"
    Finished: FAILURE

You need to add a folder through jenkins named appropriately, in this case: Monitoring.
Different seeders require different folders so the folder you need to create may vary.

New Item -> Folder

    Item name: Monitoring

4. Failed to find python installations

An example error might look like:

    FATAL: failed to find the Python installation to use from its name: System-CPython-3.5 (was it deleted?)

You need to set the python installations, or make sure you have the correct name for an existing one.

Manage Jenkins -> Global Tool Configuration -> Python -> Python installations

    Name = PYTHON_3.5

    Home or executable = /usr/bin/python3.5

    Check Install automatically

5. Weird shell errors, like Illegal option -o pipefail

You may get error output like this.

    + set -exuo pipefail
    /tmp/shiningpanda3607488247320642839.sh: 3: set: Illegal option -o pipefail
    Build step 'Virtualenv Builder' marked build as failure

In the docker container /bin/sh is dash instead of bash. Jenkins assumes that
it's bash. To fix this go to Manage Jenkins -> Configure System and set
"Shell executable" to "/bin/bash"

## WIP: Updating the Tools Jenkins Docker Image

The [edxops/tools_jenkins](https://hub.docker.com/r/edxops/tools_jenkins/) image is used in the Docker steps above. The required plugins have been pre-installed on the container. Feel free to install additional plugins. If you'd like to add or modify Jenkins plugins, follow the steps below.

Note: Adding or modifying Jenkins plugin(s) will delete all of your Jenkins workspace, including jobs, credentials, and history.

Update the plugin list by changing the plugin version number(s) or by adding additional plugin(s) in the tools_jenkins Ansible role in the configuration repository on your local machine. The role is available [here](https://github.com/edx/configuration/blob/master/playbooks/roles/tools_jenkins/defaults/main.yml). Build a new Docker image that incorporates your changes by running the following command from the root of the configuration repository:

	$ docker build -f docker/build/tools_jenkins/Dockerfile -t edxops/tools_jenkins:latest .

In order for your change(s) to be reflected in the Docker container running Jenkins, you must remove the jenkins volume, which is mounted by the docker-compose.yml file. Run the following command from the jenkins-job-dsl repo to remove the volume:

	$ docker-compose down -v

Run the following command to restart a Docker container running Jenkins with update or new plugin(s).

	$ docker-compose up

To verify that your plugins were added, connect to Jenkins and click on 'Manage Jenkins' in the side menu and select 'Manage Plugins'. You can then make sure that the plugins are visible under the 'Installed' tab.

Debugging tips:
If buidling the new Docker image fails, make sure that the playbook contains the correct roles for the tasks that are being run.
