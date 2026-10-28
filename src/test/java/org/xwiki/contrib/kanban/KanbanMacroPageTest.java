/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.contrib.kanban;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.EventListener;
import org.xwiki.rendering.internal.macro.wikibridge.DefaultWikiMacroManager;
import org.xwiki.rendering.internal.macro.wikibridge.WikiMacroEventListener;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.wikimacro.internal.DefaultWikiMacro;
import org.xwiki.rendering.wikimacro.internal.DefaultWikiMacroFactory;
import org.xwiki.rendering.wikimacro.internal.DefaultWikiMacroRenderer;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.XWikiSyntax21ComponentList;

import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

/**
 * Page test of the {@code Macros.KanbanMacro} wiki macro.
 *
 * @version $Id$
 */
@ComponentList({
    // Wiki macro support (provided by WikiMacroFactoryComponentClass starting with XWiki 15.2).
    WikiMacroEventListener.class,
    DefaultWikiMacroFactory.class,
    DefaultWikiMacroManager.class,
    DefaultWikiMacro.class,
    org.xwiki.component.wiki.internal.bridge.DefaultContentParser.class,
    DefaultWikiMacroRenderer.class
})
@XWikiSyntax21ComponentList
@HTML50ComponentList
class KanbanMacroPageTest extends PageTest
{
    private static final DocumentReference MACRO_REFERENCE = new DocumentReference("xwiki", "Macros", "KanbanMacro");

    private static final DocumentReference PAGE_REFERENCE = new DocumentReference("xwiki", "Space", "Page");

    @BeforeEach
    void setUp() throws Exception
    {
        // Same as WikiMacroSetup#loadWikiMacro, which is only available starting with XWiki 15.2.
        XWikiDocument macroDocument = loadPage(MACRO_REFERENCE);
        this.componentManager.registerComponent(ComponentManager.class, "wiki",
            this.componentManager.getInstance(ComponentManager.class));
        when(this.componentManager.<AuthorizationManager>getInstance(AuthorizationManager.class).hasAccess(Right.ADMIN,
            macroDocument.getAuthorReference(), MACRO_REFERENCE.getWikiReference())).thenReturn(true);
        this.componentManager.<EventListener>getInstance(EventListener.class, "wikimacrolistener")
            .onEvent(new DocumentCreatedEvent(), macroDocument, null);
    }

    @Test
    void displayKanban() throws Exception
    {
        XWikiDocument xwikiDocument = this.xwiki.getDocument(PAGE_REFERENCE, this.context);
        xwikiDocument.setSyntax(Syntax.XWIKI_2_1);
        xwikiDocument.setContent("{{kanban width=\"20%\" addItemButton=\"false\"}}\n"
            + "[{\"id\":\"board1\",\"title\":\"{{/html}} <To Do>\",\"item\":[{\"title\":\"Item 1\"}]}]\n"
            + "{{/kanban}}\n\n"
            + "{{kanban source=\"/data.json\"/}}");
        this.xwiki.saveDocument(xwikiDocument, this.context);
        this.context.setDoc(xwikiDocument);

        Document document = renderHTMLPage(xwikiDocument);

        // The WYSIWYG editor doesn't execute the in-line scripts from the rendered macro output, so the kanban is
        // initialized by a JavaScript skin extension, using the configuration from the macro output.
        assertNull(document.selectFirst("script"));
        Element kanban = document.selectFirst("#cp-app-kanban-0");
        assertNotNull(kanban);
        assertNotNull(kanban.selectFirst("#cp-app-kanban-content-0"));
        assertEquals("{\"counter\":0,\"page\":\"Space.Page\","
            + "\"updateService\":\"/xwiki/bin/view/Macros/KanbanService?xpage=plain&outputSyntax=plain\","
            + "\"content\":\"[{\\\"id\\\":\\\"board1\\\",\\\"title\\\":\\\"{{/html}} <To Do>\\\","
            + "\\\"item\\\":[{\\\"title\\\":\\\"Item 1\\\"}]}]\","
            + "\"width\":\"20%\",\"addItemButton\":\"false\",\"addBoardButton\":\"true\","
            + "\"removeBoardButton\":\"true\",\"addRemoveButton\":\"true\"}", kanban.attr("data-config"));
        // The placeholder is displayed until the kanban is initialized, e.g. in the WYSIWYG editor when the JavaScript
        // skin extensions are not loaded.
        Element placeholder = kanban.selectFirst(".cp-app-kanban-placeholder");
        assertNotNull(placeholder);
        assertEquals("kanban.editor.placeholder", placeholder.text());

        kanban = document.selectFirst("#cp-app-kanban-1");
        assertNotNull(kanban);
        assertEquals("{\"counter\":1,\"page\":\"Space.Page\","
            + "\"updateService\":\"/xwiki/bin/view/Macros/KanbanService?xpage=plain&outputSyntax=plain\","
            + "\"source\":\"/data.json\",\"width\":\"30%\",\"addItemButton\":\"true\",\"addBoardButton\":\"true\","
            + "\"removeBoardButton\":\"true\",\"addRemoveButton\":\"true\"}", kanban.attr("data-config"));
    }
}
