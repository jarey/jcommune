/**
 * Copyright (C) 2011  JTalks.org Team
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package org.jtalks.jcommune.plugin.auth.poulpe;

import com.google.common.collect.ImmutableMap;
import org.jtalks.jcommune.model.entity.PluginConfiguration;
import org.jtalks.jcommune.model.entity.PluginConfigurationProperty;
import org.jtalks.jcommune.model.plugins.Plugin;
import org.jtalks.jcommune.model.plugins.exceptions.NoConnectionException;
import org.jtalks.jcommune.model.plugins.exceptions.UnexpectedErrorException;
import org.jtalks.jcommune.plugin.auth.poulpe.service.PoulpeAuthService;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.*;

import static org.jtalks.jcommune.model.entity.PluginConfigurationProperty.Type.STRING;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class PoulpeAuthPluginTest {

    @Mock
    PoulpeAuthService service;

    PoulpeAuthPlugin plugin;

    @BeforeMethod
    public void setUp() throws Exception {
        initMocks(this);
        plugin = new PoulpeAuthPlugin();
        plugin.setPluginService(service);
    }

    @Test
    public void testConfigure() throws Exception {
        PluginConfiguration configuration = createConfiguration("http://localhost", "user", "1234");
        plugin.configure(configuration);

        assertTrue(plugin.getState() == Plugin.State.ENABLED,
                "Plugin with correct parameters should be configured properly.");
    }

    @Test
    public void pluginWithIncorrectParametersShouldNotBeConfigured() throws Exception {
        PluginConfiguration configuration = createConfiguration(null, "user", "1234");
        plugin.configure(configuration);

        assertTrue(plugin.getState() == Plugin.State.IN_ERROR,
                "Plugin with incorrect parameters shouldn't be configured.");
    }

    @Test
    public void userShouldNotBeRegisteredIfSomeErrorOccurred()
            throws JAXBException, IOException, NoConnectionException, UnexpectedErrorException {
        String username = "user";
        String password = "";
        String email = "";
        List<Map<String, String>> errors = new ArrayList<>();
        errors.add(new ImmutableMap.Builder<String, String>().put("test.user.email.invalid", "").build());
        errors.add(new ImmutableMap.Builder<String, String>().put("", "Invalid password").build());

        when(service.registerUser(username, password, email)).thenReturn(errors);

        List<Map<String, String>> result = plugin.registerUser(username, password, email);

        assertEquals(result.size(), 2, "User with incorrect parameters shouldn't be registered.");
    }

    @Test
    public void userWithCorrectParametersShouldBeRegistered()
            throws UnexpectedErrorException, NoConnectionException, IOException, JAXBException {
        String username = "user";
        String password = "1234";
        String email = "email@email.em";

        when(service.registerUser(username, password, email)).thenReturn(Collections.EMPTY_LIST);

        List<Map<String, String>> result = plugin.registerUser(username, password, email);

        assertEquals(result.size(), 0, "User with correct parameters should be registered.");
    }

    @Test(expectedExceptions = NoConnectionException.class)
    public void registerUserShouldThrowNoConnectionExceptionIfPoulpeUnavailable()
            throws UnexpectedErrorException, NoConnectionException, IOException, JAXBException {
        String username = "user";
        String password = "1234";
        String email = "email@email.em";

        when(service.registerUser(username, password, email)).thenThrow(new NoConnectionException());

        plugin.registerUser(username, password, email);
    }

    @Test(expectedExceptions = UnexpectedErrorException.class)
    public void registerUserShouldThrowUnexpectedErrorExceptionIfSomeErrorOccurred()
            throws UnexpectedErrorException, NoConnectionException, IOException, JAXBException {
        String username = "user";
        String password = "1234";
        String email = "email@email.em";

        when(service.registerUser(username, password, email)).thenThrow(new JAXBException(""));

        plugin.registerUser(username, password, email);
    }

    @Test
    public void userShouldNotBeAuthenticatedIfSomeErrorOccurred()
            throws JAXBException, IOException, NoConnectionException, UnexpectedErrorException {
        String username = "user";
        String password = "";

        when(service.authenticate(username, password)).thenReturn(Collections.<String, String>emptyMap());

        Map<String, String> result = plugin.authenticate(username, password);

        assertEquals(result.size(), 0, "User with incorrect parameters shouldn't be authenticated.");
    }

    @Test
    public void userWithCorrectParametersShouldBeAuthenticated()
            throws UnexpectedErrorException, NoConnectionException, IOException, JAXBException {
        String username = "user";
        String password = "1234";
        Map<String, String> authInfo = new HashMap<>();
        authInfo.put("username", "user");
        authInfo.put("password", "1234");
        authInfo.put("email", "email@email.em");

        when(service.authenticate(username, password)).thenReturn(authInfo);

        Map<String, String> result = plugin.authenticate(username, password);

        assertEquals(result.size(), 3, "User with correct parameters should be authenticated.");
    }

    @Test(expectedExceptions = NoConnectionException.class)
    public void authenticateShouldThrowNoConnectionExceptionIfPoulpeUnavailable()
            throws UnexpectedErrorException, NoConnectionException, IOException, JAXBException {
        String username = "user";
        String password = "1234";

        when(service.authenticate(username, password)).thenThrow(new NoConnectionException());

        plugin.authenticate(username, password);
    }

    @Test(expectedExceptions = UnexpectedErrorException.class)
    public void authenticateShouldThrowUnexpectedErrorExceptionIfSomeErrorOccurred()
            throws UnexpectedErrorException, NoConnectionException, IOException, JAXBException {
        String username = "user";
        String password = "1234";

        when(service.authenticate(username, password)).thenThrow(new JAXBException(""));

        plugin.authenticate(username, password);
    }

    private PluginConfiguration createConfiguration(String url, String login, String password) {

        PluginConfigurationProperty urlProperty =
                new PluginConfigurationProperty("URL", STRING, url);
        urlProperty.setName("Url");
        PluginConfigurationProperty loginProperty = new PluginConfigurationProperty("LOGIN",STRING, login);
        loginProperty.setName("Login");
        PluginConfigurationProperty passwordProperty = new PluginConfigurationProperty("PASSWORD", STRING, password);
        passwordProperty.setName("Password");
        return new PluginConfiguration("Poulpe Auth Plugin", true,
                Arrays.asList(urlProperty, loginProperty, passwordProperty));
    }
}