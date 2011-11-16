/*
 * Copyright 2011 Splunk, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"): you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.splunk.sdk.tests.com.splunk;

import com.splunk.*;
import com.splunk.sdk.Command;
import com.splunk.Service;

import junit.framework.Assert;
import junit.framework.TestCase;
import org.junit.*;

public class ApplicationTest extends TestCase {
    Command command;

    public ApplicationTest() {}

    Service connect() {
        return Service.connect(command.opts);
    }

    private Service waitForSplunk() throws Exception {
        // there is still a race condition here: if the restart takes more
        // than 5 seconds to percolate through splunk, apps will not be
        // reset
        int retry = 10;
        while (retry > 0) {
            Thread.sleep(5000); // 5 seconds
            retry = retry-1;
            try {
                return connect();
            }
            catch (Exception e) {
                // server not back yet
            }
        }
        Assert.fail("Splunk service did not restart");
        return null;
    }

    private Service cleanApp(String appName, Service service) throws Exception {
        service.restart();
        service = waitForSplunk();
        EntityCollection<Application> apps = service.getApplications();
        apps.remove(appName);
        return waitForSplunk();
    }

    @Before public void setUp() {
        command = Command.splunk(); // Pick up .splunkrc settings
    }

    // Nota Bene: Splunk needs to be restarted whenever an app is deleted
    // (more precisely, needs to be restarted after an app is deleted, AND
    // you want to manipulate, or create a new one in its place).
    // This test assumes the worst case, and will restart splunk
    // before an after an application deletion -- to correct for invalid
    // splunk application state.
    @Test public void testApps() throws Exception {
        Service service = connect();

        EntityCollection<Application> apps = service.getApplications();

        if (apps.containsKey("sdk-tests")) {
            service = cleanApp("sdk-tests", service);
        }

        apps = service.getApplications();
        Assert.assertEquals(false, apps.containsKey("sdk-tests"));

        apps.create("sdk-tests");
        Assert.assertEquals(true, apps.containsKey("sdk-tests"));
        Application app = apps.get("sdk-tests");

        app.getCheckForUpdates();
        app.getLabel();
        app.getVersion();
        app.isConfigured();
        app.isManageable();
        app.isVisible();

        // archive (package) the application
        ApplicationArchive appArchive = app.archive();
        Assert.assertTrue(appArchive.getAppName().length() > 0);
        Assert.assertTrue(appArchive.getFilePath().length() > 0);
        Assert.assertTrue(appArchive.getUrl().length() > 0);

        ApplicationSetup appSetup = app.setup();
        try {
            Assert.assertTrue(appSetup.getSetupXML().length() > 0);
        } catch (Exception e) {
            // silent exception, we expect a 500 error because the
            // setup.xml file will be missing
        }

        ApplicationUpdate appUpdate = app.update();
        Assert.assertTrue(appUpdate.getContent().containsKey("eai:acl"));

        // UNDONE: (like python tests) are we exposing 'author' in AtomObjects
        //Entity app = apps.get("sdk-tests");
        //Assert.assertFalse(app.getContent().get("author").equals("Splunk"));
        //Args map = new Args();
        //map.put("author", "Splunk");
        //app.update(map);
        //Assert.assertTrue(app.getContent().get("author").equals("Splunk"));

        service = cleanApp("sdk-tests", service);
        apps = service.getApplications();
        Assert.assertEquals(false, apps.containsKey("sdk-tests"));
    }
}