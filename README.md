# Productivity Self Delusion Machine

A tool to facilitate one's self delusion of being productive.

## Setup

After cloning the repository, run the following from the project root:

```
./script/setup
```

This will setup the development database and populate it with dummy data. You
can always rerun this script to return the project to a fresh state.

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

To reload namespaces and restart the application execute the following:

```
(reset)
```
