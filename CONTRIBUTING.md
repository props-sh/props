# Contributing to props

Thank you for your interest in contributing to this library!

# Developer set-up

## Prerequisites and Tools

- [Git](https://git-scm.com/) (usually provided by the OS)
- [SDKman!](https://sdkman.io/) or a similar tool that can manage JDK versions and the Gradle build
  system
- an IDE or text editor (we use [IntelliJ](https://www.jetbrains.com/idea/))

## Get the code

- Fork the [repository](https://github.com/props-sh/props)
- Clone your forked repository locally (`git clone https://github.com/[USERNAME]/props`)
- Add this repo as an upstream: `git remote add upstream https://github.com/props-sh/props`

## Install the Git Hooks

Before writing any code, install the project's git hooks with `gradle gitHooks`, ensuring that any
code you contribute is properly formatted and checked for any errors.

## Usual actions

- Build the project with: `gradle build`
- Run the tests with `gradle test`
- See a complete list by running `gradle tasks` or `gradle tasks --all`

# Contributing

- Please [create a GitHub issue](https://github.com/props-sh/props/issues/new)
- Then open a Pull Request and tag the newly created issue (e.g., `Fixes #12345`)
- For PRs to be accepted, you must agree to contribute under the project's [license](./LICENSE)

**Reviewers will ensure that all requirements are met before merging PRs.**

## Releases

The projects follows [semantic versioning](https://semver.org/) (MAJOR.MINOR.PATCH):

- MAJOR version when you make incompatible API changes,
- MINOR version when you add functionality in a backwards compatible manner, and
- PATCH version when you make backwards compatible bug fixes.

# License

Copyright (c) 2021 Mihai Bojin

Licensed under the MIT License

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
associated documentation files (the "Software"), to deal in the Software without restriction,
including without limitation the rights to use, copy, modify, merge, publish, distribute,
sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial
portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES
OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
