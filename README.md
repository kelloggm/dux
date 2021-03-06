# dux

A dependency orchestration tool.

## Building from source
Dux uses Google's Bazel as its build system. Follow Google's installation
instructions here: https://docs.bazel.build/versions/master/install.html 
to get Bazel on your machine. Note: we have had problems installing Bazel on 
Fedora (although it worked on CentOS).

The IDE support for Bazel does not seem to be that mature right now (at least 
for IntelliJ), so we have just been using Bazel from the command line.

Once you have Bazel installed, run `bazel build //:dux` from the top-level
directory. The resulting output is in the /output directory. To run the 
program, run `./bazel-bin/dux [options]`. A typical use case would be:

First, demonstrate a build to dux:
> ./bazel-bin/dux -b "bucket_name" -c command_to_trace

Then, check dependencies on another computer after copying over the config file:
> ./bazel-bin/dux -b "bucket_name" -k

This will not do anything meaningful until you set up a cloud backend for dux.
See /credentials/README.md for details.

## Testing
Dux also uses Travis-CI for testing. Again, see /credentials/README.md for info
on how to configure your credentials with Travis.

