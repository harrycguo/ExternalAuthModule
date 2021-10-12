package external;

import net.shibboleth.idp.attribute.context.AttributeContext;
import net.shibboleth.idp.authn.ExternalAuthentication;
import net.shibboleth.idp.authn.ExternalAuthenticationException;
import net.shibboleth.idp.authn.context.SubjectCanonicalizationContext;
import net.shibboleth.idp.authn.principal.UsernamePrincipal;
import net.shibboleth.idp.profile.context.RelyingPartyContext;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import java.util.function.Function;


@WebServlet(name = "ExternalAuth", urlPatterns = {"/Authn/External/*"})
public class ExternalAuth extends HttpServlet {

    // logger for debugging
    // can also just System.out.println() for debugging, will print to IDP jetty logs
    private final Logger logger = LoggerFactory.getLogger(ExternalAuth.class);

    /** Lookup function for relying party context. */
    protected Function<ProfileRequestContext, RelyingPartyContext> relyingPartyContextLookupStrategy = new ChildContextLookup<>(
            RelyingPartyContext.class);

    /** Lookup function for subject canonicalization context. */
    protected Function<ProfileRequestContext, SubjectCanonicalizationContext> subjectCanonicalizationContextLookupStrategy = new ChildContextLookup<>(
            SubjectCanonicalizationContext.class);

    /** Lookup function for subject canonicalization context. */
    protected Function<ProfileRequestContext, AttributeContext> attributeContextLookupStrategy = new ChildContextLookup<>(
            AttributeContext.class);

    @Override
    public void init(final ServletConfig config) throws ServletException {
        super.init(config);
    }

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        // Redirect to our idp selection jsp view
        RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("/idp-selection.jsp");
        dispatcher.forward(request, response);
    }

    @Override
    protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        try {
            // 1. Call ExternalAuthentication.startExternalAuthentication(HttpServletRequest), saving off the result as a key.
            final String key = ExternalAuthentication.startExternalAuthentication(request);

            // 2. Do work as necessary (reading request details from the attributes below). Any redirects must preserve the key
            // value returned in step 1 because it must be used to complete the login later.
            try {
                // Grab username from the form data
                String username = request.getParameter("userName");

                // If "remember me?" is set, set the cookie for the idp host domain
                Cookie userNameCookie = new Cookie("cu_username", username);
                userNameCookie.setPath("/idp"); // path set so cookie can be reused in password flow
                userNameCookie.setMaxAge(604800); // expires in 1 week
                response.addCookie(userNameCookie);

                // Initiate a HashSet of principals. We will transform this into a subject later.
                HashSet<Principal> principals = new HashSet<Principal>();
                principals.add(new UsernamePrincipal(username));

                // Create subject to send back as a result of the External module
                request.setAttribute(ExternalAuthentication.SUBJECT_KEY, new Subject(false, principals, Collections.EMPTY_SET, Collections.EMPTY_SET));

                // 4. Call ExternalAuthentication.finishExternalAuthentication(String, HttpServletRequest, HttpServletResponse).
                // The first parameter is the key returned in step 1.
                ExternalAuthentication.finishExternalAuthentication(key, request, response);

            } catch (Exception e) {
                request.setAttribute(ExternalAuthentication.AUTHENTICATION_ERROR_KEY, e.toString());
                ExternalAuthentication.finishExternalAuthentication(key, request, response);
                return;
            }
        } catch (final ExternalAuthenticationException e) {
            throw new ServletException("Error processing external authentication request", e);

        }
    }
}
