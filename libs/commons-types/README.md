# commons-types
The `commons-types` library is a collection of types including enumerations, models, and utility classes that are commonly shared by the processing status API services.  

## Usage

### gradle
Add the following to the `dependencies` of your project' `build.gradle`.
```groovy
dependencies {
    implementation project(':libs:commons-types')
}
```
This will allow the `commons-types` to be compiled if necessary and linked with your project.  You can set breakpoints in the library the same as you would your main project for debugging.

> **_Important:_** You can't have a `settings.gradle` file in your project.  Delete it if you have one.  Otherwise, the root project `settings.gradle` will not be picked up and gradle won't be able to find the library. 
