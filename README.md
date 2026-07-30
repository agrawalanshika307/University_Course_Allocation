# University Course Allocation — Spring Boot API



A Spring Boot REST API that assigns courses to professors, given each

professor's course load and preferences, using a priority-based allocation

algorithm with automatic conflict resolution (if a preferred professor is

already full, the engine recursively tries to free them up by reassigning

their lower-priority courses first).



## Tech used

- **Spring Web** — `@RestController`, `@PostMapping`, JSON request/response binding

- **Spring Validation** — `@Valid` + `@NotBlank` on the request DTO

- **Spring exception handling** — `@RestControllerAdvice` turns bad input into a clean `400` JSON error instead of a stack trace

- **Spring Boot Test** — `@SpringBootTest` + `MockMvc` integration tests hitting the real endpoints



## Run it

```bash

mvn spring-boot:run

```



## Try it — two ways



### Option A: upload a file directly

```bash

curl -X POST http://localhost:8080/api/allocate/file -F "file=@input.txt" -o output.txt

```



### Option B: send input as a JSON string

```bash

curl -X POST http://localhost:8080/api/allocate 

&#x20; -H "Content-Type: application/json" 

&#x20; -d '{"inputText": "7npC,0.5,0,0,1,fde1,0npD,1,0,0,1,fde1,1,hde1npB,1,0,1,hdc1,1,fde1,0npE,0.5,1,fdc1,1,hdc1,0,0npA,1,2,fdc1,fdc2,0,0,0npF,1,1,fdc1,0,0,0npG,1,1,fdc1,0,0,0n"}'

```



## Run the tests

```bash

mvn test

```



## How the algorithm works

Each course is assigned to professors in priority order. If a professor is

already at capacity, the engine recursively checks whether one of that

professor's existing (lower-priority) courses can be reassigned elsewhere,

freeing them up for the higher-priority course — backtracking if no valid

reassignment exists. Course loads are tracked in half-course units, so a

professor can be assigned a mix of full and half courses up to their total

capacity.

