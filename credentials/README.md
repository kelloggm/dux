# Getting the credentials
In order to contact a Dux file server, you will need some credentials.

Right now, Dux only supports a file server backed by Google Cloud Storage. 
To authenticate yourself, create a service account for your cloud datastore
and then set the environment variable GOOGLE_APPLICATION_CREDENTIALS to the
json file you downloaded (the name of your file will be different):

> export GOOGLE_APPLICATION_CREDENTIALS=mygoogleauth.json


# Integrating credentials into Travis
This project also uses Travis-CI for tests. If you intend to push to this repo
(or a fork) and expect the CI to work, you will need to do a couple special 
things with your credentials. 

First, enable the GitHub project on your Travis account (if you don't have one,
it's free to sign up with your GitHub login here: https://travis-ci.org/).

Next, you will need to store an encrypted version of your credentials on GitHub.
The tests currently expect your credentials to be located in /credentials and 
named GOOGLE_APPLICATION_CREDENTIALS. It's easiest if you just follow this
convention, so copy your credentials there and reset the environment variable
if necessary.

The full procedure for the setup is here: 
https://cloud.google.com/solutions/continuous-delivery-with-travis-ci, but
the shortened version below should be enough.

1.) Install Travis.
> gem install travis

2.) Log in to Travis. You'll be prompted for your GitHub login credentials:

> travis login

3.) Encrypt the file locally. If prompted to overwrite the existing file, respond yes:

> travis encrypt-file GOOGLE_APPLICATION_CREDENTIALS --add

It's the --add option that causes the Travis client to automatically add the
decryption step to the before_install step in the .travis.yml file.

4.) Check the .travis.yml file. Move and replace the existing `openssl`
instruction with the newly generated one (probably at the bottom of the file).
However, make sure you keep the file paths and names the same and mimic the
structure that the old `openssl` call used.

5.) Add the encrypted credentials to the repo:

> git add credentials/GOOGLE_APPLICATION_CREDENTIALS.enc .travis.yml\
> git commit -m "Adds encrypted credentials for Travis"

Note: You currently also need to replace a hardcoded Google cloud bucket name
in src/dux/cli/DuxOptions.java with your own bucket name.