# ExternalAuthModule

This is an example of an External Auth module to be plugged into a Shibboleth Identity Provider. 

It is a simple External Auth that does 2 things:

1) Redirect the User to a custom JSP webpage with a form for a username
2) Takes the username from the form and creates a Java Subject from it. Setting the username as the
Username Principal.
   
NOTE: Your custom webpage will need to match the file name that is in the `GET` method. Your form will also need
to submit the correct parameter in the `POST` as well (`userName` in this instance).




### Using the External Module

#### Create the jar and add to Shibboleth IDP

I recommend IntelliJ for this. 

1. Build Project
2. File > Project Structure > Project Settings > Artifacts
3. Hit the + > JAR > From project with dependencies > OK

(Next part is a bit tricky and something I don't know how to do yet via code/commands)

4. Move your jar into a folder and run `jar xf <JAR NAME>.jar`
5. Remove all files except for `external/ExternalAuth.class` and the `META-INF/MANIFEST.MF`.
There is an example jar directory `example-jar-directory`. Instead of removing files you can also just add
   the necessary ones into a new folder.
6. Jar it back up with a name `jar cvf <JAR NAME>.jar -C <FOLDER-NAME>/ .`


#### Shibboleth IDP config

Make sure `authn/External` bean is enabled in `general-authn.xml` and that the `external-authn-config.xml`
is present.

Make sure your JSP webpage is in `webapp/` directory.

In `web.xml`, add the following servlet:

```xml
<!-- Servlet for receiving a callback from an external Server and continues the IdP login flow -->
 <servlet>
    <servlet-name>ExternalAuth</servlet-name>
    <servlet-class>external.ExternalAuth</servlet-class>
    <load-on-startup>2</load-on-startup>
</servlet>
<servlet-mapping>
    <servlet-name>ExternalAuth</servlet-name>
    <url-pattern>/Authn/External/*</url-pattern>
</servlet-mapping>
```


