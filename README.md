# Productivity Self Delusion Machine

A tool to facilitate one's self delusion of being productive.

This project makes use of GitHub's [Scripts To Rule Them All]
(https://github.com/github/scripts-to-rule-them-all)
to provide a consistent interface for working with this project irrespective of
the build tools it uses under the hood.

## Setup

After cloning the repository, run the following from the project root:

```
./script/setup
```

This will setup the development database and populate it with dummy data. It
will also compile the ClojureScript sources. You can always rerun this script to
return the project to a fresh state.

Every time you pull down changes to this repo, you should also run the following
from the project root:

```
./script/update
```

This will run any new database migrations and recompile the ClojureScript.

## Run

From the project root, run the following:

```
./script/server
```

Then navigate to [http://localhost:5000](http://localhost:5000).

## Tests

From the project root, run the following:

```
./script/test
```

## Interactive Development

You cat start up a repl by running the following from the project root:

```
./script/console
```

To start the application, execute the following at the repl:

```
(go)
```

To reload namespaces and restart the application, execute the following:

```
(reset)
```
