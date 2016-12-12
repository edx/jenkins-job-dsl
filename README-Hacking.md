# Local Development

The simplest way to get started with local development is to use our Docker container. This container is pre-configured 
to run Jenkins. The `docker-compose.yml` file in the root of this repository contains the configuration necessary to 
bring a new container online with the proper ports exposed, and volumes shared.

Execute this command from the root of the repository:

    $ docker-compose up

If you opt not to use `docker-compose`, the following command will achieve the same results.

    $ docker run -p 8080:8080 -p 50000:50000 -v .docker-volumes/jenkins:/var/jenkins_home edxops/jenkins

In both instances ports 8080 and 50000 will be exposed to the host system, and a local volume will be shared on the 
container. This local volume contains all of the configuration for Jenkins, and will be preserved between container
stops and starts.

**NOTE:** You are not required to use `.docker-volumes/jenkins`, and can use practically any directory on your local 
filesystem. However, due to the nature of how Docker runs on Mac OS X, you can only share sub-directories of your
home directory when using Mac OS X.

Once the container is running, you can connect to Jenkins at `http://localhost:8080`.

In order to bootstrap Jenkins in a container you will need to do two things:

1. Set up any credentials required for pulling private repositories.
2. Set up a seed job to process the job DSL.

### Credentials

You will need credentials--SSH key, or username and password--to clone private Git repositories. If you are using 
 GitHub, use a scoped [personal access token](https://github.com/settings/tokens) to limit potential security risks.  

Credentials can be added using the Jenkins UI.

**NOTE:** The alternative below is still a work in progress.

Alternatively, you can create a YAML file with your credentials, and execute a script to register the credentials with 
Jenkins. An example of the YAML file is available at `.docker-volumes/jenkins/jenkins-credentials.example.yaml`.

Follow these steps to add your credentials.

1. Create a new file, `.docker-volumes/jenkins/jenkins-credentials.yaml`, and add your credentials.
2. Execute the command below. Remember to change the URL if you are using Mac OS X.


    $ java -jar .docker-volumes/jenkins/war/WEB-INF/jenkins-cli.jar -s http://localhost:8080 groovy resources/addCredentials.groovy


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

The [edxops/jenkins](https://hub.docker.com/r/edxops/jenkins/) image is used in the Docker steps above. If you'd like to
 add or modify Jenkins plugins, update `docker/plugins.txt`.
 
The required plugins have been pre-installed on the container. Feel free to install additional plugins, but
if they are required for either other developers or deployment further steps will be required.  To add them to the 
the Docker container, they must be listed in ```docker/plugins.txt```. To be installed in a deployment environment,
they must be added to the appropriate Ansible defaults.
