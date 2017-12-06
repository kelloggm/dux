#include <stdio.h>
#include <string.h>
#include <readline/readline.h>
#include <readline/history.h>

#include <stdlib.h>
#include <ctype.h>
#include <unistd.h>
#include <sys/wait.h>

const int MAX_PROCS = 100;

/* Borrowed from https://stackoverflow.com/questions/122616/how-do-i-trim-leading-trailing-whitespace-in-a-standard-way
 * This really should be in the standard library...
 */
char *trim(char *str) {
  char *end;

  // Trim leading space
  while(isspace((unsigned char)*str)) str++;

  if(*str == 0)  // All spaces?
    return str;

  // Trim trailing space
  end = str + strlen(str) - 1;
  while(end > str && isspace((unsigned char)*end)) end--;

  // Write new null terminator
  *(end+1) = 0;

  return str;
}

/* For a child process, replace stdin and stdout
 * with the specified input (reading) fd and output (writing) fd.
 */
void replaceStdio(int inFd, int outFd) {
  if (dup2(inFd, STDIN_FILENO) == -1) {
    fprintf(stderr, "Problem replacing stdin\n");
  }
  if (dup2(outFd, STDOUT_FILENO) == -1) {
    fprintf(stderr, "Problem replacing stdout\n");
  }
}

/* Close the file as long as it's not stdin, stdout, or invalid (-1). */
void safeClose(int fd) {
  if (fd == STDIN_FILENO || fd == STDOUT_FILENO || fd == -1) {
    return;
  }
  #ifdef DEBUG
  fprintf(stderr, "Closing %d\n", fd);
  #endif

  if (close(fd) == -1) {
    fprintf(stderr, "Invalid close\n");
  }
}

/* forkOffProc:
 * 1. forks a new process, and replaces stdin and stdout in the child with the given fd_s
 * 2. closes all open fd_s in the child (besides stdin and stdout)
 * 3. the child execs, parent continues. Fd_s are still open in the parent.
 */
int forkOffProc(char* proc, int inFd, int outFd, int nextFd) {
  char* args[2];
  args[0] = proc;
  args[1] = NULL;

  pid_t pid = fork();
  if (pid == -1) {
    fprintf(stderr, "Fork failed\n");
    return -1;
  } else if (pid == 0) {
    /* child */
    replaceStdio(inFd, outFd);

    // Close these fd_s because a reading pipe is only considered closed if all open
    // copies of the writing end are closed: https://stackoverflow.com/a/19265380
    safeClose(inFd);
    safeClose(outFd);
    safeClose(nextFd);

    if (execvp(proc, args) == -1) {
      fprintf(stderr, "Could not exec child \"%s\"\n", proc);
      exit(EXIT_FAILURE); /* So the child doesn't continue as a shell. */
    }
  } else if (pid > 0) {
    /* parent */
    return 0;
  } else {
    fprintf(stderr, "Something very bad\n");
    return -1;
  }
  return 0; // never reached
}

/* Reads input commands into token array.
 * Returns -1 on error, 0 for all whitespace (that's okay), or number of processes <= maximum.
 */
int parseCommands(char *input, char *tokens[MAX_PROCS]) {
  const char* DELIMITER = "|";
  int i = 0;

  for (char* token = strtok(input, DELIMITER); token != NULL; token = strtok(NULL, DELIMITER)) {
    if (i >= MAX_PROCS) {
      fprintf(stderr, "Too many processes, expected at most %d\n", MAX_PROCS);
      return -1;
    }

    token = trim(token);
    tokens[i] = token;

    // Inappropriate blanks: Blanks before or after pipes are wrong (i.e.,
    // previous token was blank or current token is blank if i > 0).
    // It is okay for the very first token to be blank because the whole line
    // could be blank in that case.
    if (i > 0 && (*token == '\0' || *tokens[i - 1] == '\0')) {
      fprintf(stderr, "Parse error: no command between pipes\n");
      return -1;
    }

    i++;
  }

  // single blank line
  if (*tokens[0] == '\0') {
    return 0;
  }

  return i;
}

/* Parses the input, forks off the appropriate
 * number of child processes, sets up pipes between
 * them, and then waits for them all to finish.
 */
void processLine(char *input) {
  char *tokens[MAX_PROCS];

  /* parse input and count commands */
  int numProcs = parseCommands(input, tokens);
  if (numProcs == -1 || numProcs == 0) {
    return;
  }

  /* fork off child processes */
  int numProcsRunning = 0;
  int inFd = STDIN_FILENO;
  int outFd, nextFd;
  for (int i = 0; i < numProcs; i++) {
    char* token = tokens[i];
    
    #ifdef DEBUG
    fprintf(stderr, "%s\n", token);
    fflush(stderr);
    #endif

    if (i != numProcs - 1) {
      // creating a pipe:
      // forked child reads from read end of last pipe (or stdin at start)
      // writes to write end of current pipe (or stdout at end)
      int tempPipe[2];
      if (pipe(tempPipe) == -1) {
	    safeClose(inFd);
	    fprintf(stderr, "Failed to pipe\n");
	    break;
      }
      #ifdef DEBUG
      fprintf(stderr, "Opening fds %d, %d at %d\n", tempPipe[0], tempPipe[1], i);
      #endif

      outFd = tempPipe[1];
      nextFd = tempPipe[0];
    }
    else { // last process writes to stdout
      outFd = STDOUT_FILENO;
      nextFd = -1;
    }
  
    int forked = forkOffProc(token, inFd, outFd, nextFd);
    // once we've forked, we can close fd's because parent doesn't use them,
    // also so that children's pipes close as soon as possible (reading pipes
    // are not considered closed until all copies of the writing end are closed)
    safeClose(inFd);
    safeClose(outFd);
    if (forked == -1) {
      fprintf(stderr, "Problem forking process %s\n", token);
      safeClose(nextFd);
      break;
    }

    inFd = nextFd; 
    numProcsRunning++;  
  }
  
  /* processes all execute concurrently, we wait one by one */
  while (numProcsRunning) {
    // in principle an error would only happen here if there are no processes to wait on
    if (wait(NULL) == (pid_t)-1) {
      fprintf(stderr, "Problem waiting\n");
      return;
    }
    numProcsRunning--;
  }
}

int main(int argc, char** argv) {
  // Exciting, fancy prompt!
  const char* PROMPT = "!!!>";

  char* input;
  while ((input = readline(PROMPT)) != NULL) {
    #ifdef DEBUG
    fprintf(stderr, "%s\n", input);
    fflush(stderr);
    #endif

    processLine(input);
    free(input); /* readline mallocs so we must free */
    fflush(stderr);
  }

  return EXIT_SUCCESS;
}
