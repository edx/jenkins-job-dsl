# Local Development

A `docker-compose.yml` file is included to aid local development. This file is configured to use the [official Jenkins
 Docker image](https://hub.docker.com/_/jenkins/) with data stored in a data volume.
 
Start by running this command from the root of the repository:

    $ docker-compose up

Go to `http://localhost:8080` in your browser and follow the on-screen instructions to login complete the initial 
configuration of Jenkins. When asked about plugins, choose the recommended set of plugins.

It is up to you if you want to create a new administrative user. Since this is not going to production, you might find 
it simpler to continue using the default `admin` account but assign it memorable password at 
http://localhost:8080/user/admin/configure.

Once Jenkins has been configured, create the seed job which will be used to run the DSL and create other jobs. From the 
root of the repository run the following command. Remember to use the username/password from the initial configuration.

    $ ./gradlew rest -Dpattern=jobs/seed.groovy -DbaseUrl=http://localhost:8080/api -Dusername=admin -Dpassword=admin

### Credentials

You will need credentials--SSH key, or username and password--to clone private Git repositories. If you are using 
 GitHub, use a scoped [personal access token](https://github.com/settings/tokens) to limit potential security risks.  

Credentials can be added using the Jenkins UI.

Note that if using a personal access token, you can only clone https://github.com/ URLs, and ssh keys only work on 
git@github.com:edx URLs.  We use git@github.com URLs on tools-edx-jenkins with deployment keys, but you can use 
whatever is simpler in testing.
