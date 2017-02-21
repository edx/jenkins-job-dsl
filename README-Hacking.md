# Local Development

The simplest way to get started with local development is to use our Docker container. This container is pre-configured 
to run Jenkins. The `docker-compose.yml` file in the root of this repository contains the configuration necessary to 
bring a new container online with the proper ports exposed, and volumes shared.

Execute this command from the root of the repository:

    $ docker-compose up

If you opt not to use `docker-compose`, the following command will achieve the same results.

    $ docker run -p 8080:8080 -v jenkins:/edx/var/jenkins tools_jenkins

In both instances port 8080 will be exposed to the host system, and a volume will be created on the 
container. This volume contains all of the configuration for Jenkins, and will be preserved between container
stops and starts, except in the case of plugin updates or installs (see WIP: Updating the Docker Image below).

Once the container is running, you can connect to Jenkins at `http://localhost:8080`.

In order to bootstrap Jenkins in a container you will need to do two things:

1. Set up any credentials required for pulling private repositories.
2. Set up a seed job to process the job DSL.

### Credentials

You will need credentials--SSH key, or username and password--to clone private Git repositories. If you are using 
 GitHub, use a scoped [personal access token](https://github.com/settings/tokens) to limit potential security risks.  

Credentials can be added using the Jenkins UI.

### Testing DSL

You will need to create a new Jenkins job that executes the DSL, and creates other jobs. This can be done with the steps
 below.

1. Use the Jenkins UI to create a new **Freestyle project** job named **Job Creator**.
2. Configure the job to use **Multiple SCMs** for *Source Control Management*, and add a Git repository. (Note that we 
are NOT using the Git plugin here.)
    1. Set the Repository URL to the repo containing your job DSL (e.g. git@github.com:edx/jenkins-job-dsl-internal.git).
    2. If necessary, select your authentication credentials (e.g. SSH key).
3. Add a **Process Job DSLs** build step and configure it using the settings below. Remember to click the  *Advanced* 
button to expose the final two fields.
    1. DSL Scripts: jobs/hacking-edx-jenkins.edx.org/*Jobs.groovy 
       (You may opt to change this if you're developing for a different Jenkins server.)
    2. Action for existing jobs and views: UNchecked
    3. Action for removed jobs: Delete
    4. Action for removed views: Ignore
    5. Context to use for relative job names: Jenkins Root
    6. Additional classpath: src/main/groovy
4. Save the job, and run it.


## WIP: Updating the Docker Image

The [edxops/tools_jenkins](https://hub.docker.com/r/edxops/tools_jenkins/) image is used in the Docker steps above. The required plugins have been pre-installed on the container. Feel free to install additional plugins. If you'd like to add or modify Jenkins plugins, follow the steps below.

Note: Adding or modifying Jenkins plugin(s) will wipe your Jenkins workspace.

Update the plugin list by changing the plugin version number(s) or by adding additional plugin(s) in the tools_jenkins Ansible role in the configuration repository on your local machine. The role is available [here](https://github.com/edx/configuration/blob/master/playbooks/roles/tools_jenkins/defaults/main.yml). Build a new Docker image that incorporates your changes by running the following command from the root of the configuration repository:

	$ docker build -f docker/build/tools_jenkins/Dockerfile -t edxops/tools_jenkins:latest .

In order for your change(s) to be reflected in the Docker container running Jenkins, you must remove the jenkins volume, which is mounted by the docker-compose.yml file. Run the following command to remove the volume:

	$ docker-compose down -v

Run the following command to restart a Docker container running Jenkins with update or new plugin(s).

	$ docker-compose up
