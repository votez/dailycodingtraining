# Purpose
Offers algo problems to solve to train your skills. The problems are from [Daily Coding Problem](https://www.dailycodingproblem.com).

# Usage
Start the script
```shell
mvn spring-boot:run
```

in there you have the following commands :
* *help* : shows help for commands
* *next easy* OR *easy*: shows a next easy level problem
* *next medium* OR *medium*: shows a next medium level problem
* *next hard* OR *hard* : shows a next hard problem
* *next*: shows a next problem of the same difficulty level as previous
* *done*: mark the current problem as done, will not be asked again
* *skip*: skip the current problem and mark as ignored, will not be asked again
* *reset solved database*: reset the solved/skipped database so all problems will be asked again 

To postpone a problem just type *next* and it will be just ignored for this run - problems are
marked as 'not to show' only when explicitly marked *done* or *skip*.

Note the problems are presented in random order.