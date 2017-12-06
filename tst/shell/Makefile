all: clean build

clean:
	rm -f 550shell

build:
	gcc -Wall -lreadline shell.c -o 550shell

debug:
	gcc -Wall -lreadline -D DEBUG shell.c -o 550shell
