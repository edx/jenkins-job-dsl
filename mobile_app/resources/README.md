Mobile Apps
===========

Tools contained here are used for working with the mobile-apps.


Setup
-----
To run any of these scripts, you will first need to:
* Create your virtual environment
* ``pip install -r testeng-ci/requirements.txt``

The following scripts constitute a build pipeline for the edX mobile apps.

make_android_build
------------------
Invokes ``make_build``, filling in parameters specific to the edx android app.

* Call script as a python module, e.g.

	`python -m mobile_app.make_android_build`

make_ios_build
------------------
Invokes ``make_build``, filling in parameters specific to the edx ios app.

* Call script as a python module, e.g.

	`python -m mobile_app.make_ios_build`



make_build
----------------
This contains code to generate a new environment and command line for the
``trigger_build`` task (see below) by asking the user to enter that
information manually.  It is meant to stand in for a task runner like Jenkins
when doing local testing. Once the user has entered all the relevant
information, it invokes ``trigger_build`` with those values.

*Usage*

* Call script as a python module, e.g.

	`python -m mobile_app.make_build`


trigger_build
-------------
This will:

* Write a known set of environment variables to disk in JSON format to a file
  named CONFIGURATION.
* Create a new commit with that environment and push to origin.

The build repo should be configured so that this push will in turn trigger a CI
job which makes a new build with that commit, providing access to that
environment. The goal is to capture the environmment where the job is triggered
(like Jenkins) and save it so the actual build step can be run on a separate
build machine with access to that environment.

*Usage*

* Export the necessary environment variables. There's a list inside the script.
* Call script as a python module, e.g.

	`python -m mobile_app.trigger_build --branch-name UniqueBranchName --trigger-repo-path ../my-repo`


The expectation is that the branch name will be some unique identifier like the
jenkins job number and the date.
	

checkout_build_repos
--------------------

Checks out a code and config repository and sets them up for building, by
creating a properties file to point the code at the config. This is meant to
run on a CI machine before the build step.

*Usage*
* Create a file called ``CONFIGURATION``. See ``trigger_build`` for more information on the format.
* Call script as a python module, e.g.

    `python -m mobile_app.checkout_build_repos`

This will result in two new folders, "code.git" and "config.git" cloned from the code and config URLs in CONFIGURATION. They will be checked out to the revision specified in CONFIGURATION.

upload_build
------------
Uploads a built app for distribution + archiving. Currently builds go to HockeyApp. This is meant to run on a CI machine after the build step.

*Usage*
* Build your app somewhere
* Export the necessary environment variables. There's a list inside the script.
* Call script as a python module, e.g.
    
    `python -m mobile_app.upload_build`

