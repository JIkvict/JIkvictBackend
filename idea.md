# Idea

## Frontend

### Browser
IDE integrated in browser - the perfect solution, but hardly possible, It would be easy to implement for languages like JS or python. But java often requires a whole project with dependencies.
This means we need to give a server for each user where they can write their code. Moreover, to support syntax highlighting, we need to use a language server, and again we need to provide this server for every user.
So, this solution seems expensive as it requires a lot of resources and it is hard to implement to support all synchronization and syntax highlighting.

### Plugin
I suggest creating a plugin for IntelliJ IDEA. This way user can use all the features of the IDE and solve tasks in the familiar environment. The only thing we need to do is to create a plugin that will send the code to the server and get the result back. This way we can use the existing infrastructure of the IDE and we don't need to worry about the synchronization and other things.
Another good thing is that idea has a good platform for building plugins; this means I can implement features that will prevent users from deleting important files e.g. they won't be able to delete an interface that they need to implement.
For users that for any reason don't have idea will be another way to solve tasks, I will describe it later.

### How to create a task?
Plugin will support "teacher" mode, you will click some button like "Create new task" plugin will load new template project, where in src you will create basic interfaces and structure for the program.
Then in tests/public you will create a few tests that will be visible for students, they will cover the main functionality of the program/algorithm.
Then in test/private you will create tests that will cover the edge cases and will be hidden from students, they will be used to check the solution and assign points.
Then there will be some xml or md, where you will fill in information about the task, like name, description, points, etc.

This approach seems to bee good, as you can use any test framework you want, so not only the correctness of the algorithm can be checked, but also performance, memory usage or even the project structure, naming conventions, etc.


### Plugin - continue [backend part]
When task is "created" in idea. You can click "Send task" button in plugin. Plugin will compress a project to zip and send it to the server.
Server will decompress the project and split it into two parts:
Public part with project template and pubic tests.
And private part with tests.
Public part will be published to some public repo
Private part will be sent to some private repo.
The xml or md file will be used to create a task in the database with links to the repos.


### How to solve a task?
Students will see all available tasks in the plugin.
When a user clicks "start solving", plugin will load project structure from public repo and user can start solving the task.
When a user clicks "send solution", the plugin will compress the project and send it to the server.


## Backend

We won't use any existing solutions as they do not provide the flexibility we need.
Backend will receive ZIP with a solution, then it will merge it with the private tests and send it to the "solution checker" service

## Solution checker
This service will receive the ZIP with the solution and private tests.
It will decompress the ZIP and run the tests.
It will generate som xml or json with the results and send it to the backend.

## No plugin support
If a user does not have idea, they can use the web interface.
The web interface will have the functionality of downloading the project-template zip.
Then the user will manually unzip it and open it in any ide.
Then we will manually compress it and upload it to the server.
This approach is not very convenient, but it will work for users that for some reason don't want to use IDEA.
