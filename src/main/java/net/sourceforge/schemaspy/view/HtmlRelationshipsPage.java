/*
 * This file is a part of the SchemaSpy project (http://schemaspy.sourceforge.net).
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011 John Currier
 *
 * SchemaSpy is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * SchemaSpy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package net.sourceforge.schemaspy.view;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import net.sourceforge.schemaspy.model.Database;
import net.sourceforge.schemaspy.model.ProgressListener;
import net.sourceforge.schemaspy.model.TableColumn;
import net.sourceforge.schemaspy.util.Dot;
import net.sourceforge.schemaspy.util.LineWriter;

/**
 * The page that contains the overview entity relationship diagrams.
 *
 * @author John Currier
 */
public class HtmlRelationshipsPage extends HtmlDiagramFormatter {
    private static final HtmlRelationshipsPage instance = new HtmlRelationshipsPage();
    private TemplateService templateService;

    /**
     * Singleton: Don't allow instantiation
     */
    private HtmlRelationshipsPage() {
        templateService = TemplateService.getInstance();
    }

    /**
     * Singleton accessor
     *
     * @return the singleton instance
     */
    public static HtmlRelationshipsPage getInstance() {
        return instance;
    }

    public boolean write(Database db, File diagramDir, String dotBaseFilespec, boolean hasRealRelationships, boolean hasImpliedRelationships,
    					Set<TableColumn> excludedColumns, ProgressListener listener, LineWriter html) {
        try {
            Dot dot = getDot();
            if (dot == null) {
                writeHeader(db, null, "All Relationships", html);
                html.writeln("<div class='content'>");
                writeInvalidGraphvizInstallation(html);
                html.writeln("</div>");
                writeFooter(html);
                return false;
            }

            File compactRelationshipsDotFile = new File(diagramDir, dotBaseFilespec + ".real.compact.dot");
            File compactRelationshipsDiagramFile = new File(diagramDir, dotBaseFilespec + ".real.compact." + dot.getFormat());
            File largeRelationshipsDotFile = new File(diagramDir, dotBaseFilespec + ".real.large.dot");
            File largeRelationshipsDiagramFile = new File(diagramDir, dotBaseFilespec + ".real.large." + dot.getFormat());
            File compactImpliedDotFile = new File(diagramDir, dotBaseFilespec + ".implied.compact.dot");
            File compactImpliedDiagramFile = new File(diagramDir, dotBaseFilespec + ".implied.compact." + dot.getFormat());
            File largeImpliedDotFile = new File(diagramDir, dotBaseFilespec + ".implied.large.dot");
            File largeImpliedDiagramFile = new File(diagramDir, dotBaseFilespec + ".implied.large." + dot.getFormat());


            writeHeader(db, null, "All Relationships", html);

            GlobalData globalData = new GlobalData();
            globalData.setDatabase(db);

            RelationshipPageData data = new RelationshipPageData();
            data.setGlobalData(globalData);
            data.setHasImpliedRelationships(hasImpliedRelationships);
            data.setHasRealRelationships(hasRealRelationships);

            String header = templateService.renderTemplate("relations/localRelationshipTemplate.ftl", data);
            html.writeln(header);

            if (hasRealRelationships) {
            	listener.graphingSummaryProgressed();

                html.writeln(dot.generateDiagram(compactRelationshipsDotFile, compactRelationshipsDiagramFile));
                html.writeln("  <a name='diagram'><img id='realCompactImg' src='diagrams/summary/" + compactRelationshipsDiagramFile.getName() + "' usemap='#compactRelationshipsDiagram' class='diagram' border='0' alt=''></a>");

                // we've run into instances where the first diagrams get generated, but then
                // dot fails on the second one...try to recover from that scenario 'somewhat'
                // gracefully
                try {
                	listener.graphingSummaryProgressed();

                    html.writeln(dot.generateDiagram(largeRelationshipsDotFile, largeRelationshipsDiagramFile));
                    html.writeln("  <a name='diagram'><img id='realLargeImg' src='diagrams/summary/" + largeRelationshipsDiagramFile.getName() + "' usemap='#largeRelationshipsDiagram' class='diagram' border='0' alt=''></a>");
                } catch (Dot.DotFailure dotFailure) {
                    System.err.println("dot failed to generate all of the relationships diagrams:");
                    System.err.println(dotFailure);
                    System.err.println("...but the relationships page may still be usable.");
                }
            }

            try {
                if (hasImpliedRelationships) {
                	listener.graphingSummaryProgressed();

                    html.writeln(dot.generateDiagram(compactImpliedDotFile, compactImpliedDiagramFile));
                    html.writeln("  <a name='diagram'><img id='impliedCompactImg' src='diagrams/summary/" + compactImpliedDiagramFile.getName() + "' usemap='#compactImpliedRelationshipsDiagram' class='diagram' border='0' alt=''></a>");

                	listener.graphingSummaryProgressed();

                    html.writeln(dot.generateDiagram(largeImpliedDotFile, largeImpliedDiagramFile));
                    html.writeln("  <a name='diagram'><img id='impliedLargeImg' src='diagrams/summary/" + largeImpliedDiagramFile.getName() + "' usemap='#largeImpliedRelationshipsDiagram' class='diagram' border='0' alt=''></a>");
                }
            } catch (Dot.DotFailure dotFailure) {
                System.err.println("dot failed to generate all of the relationships diagrams:");
                System.err.println(dotFailure);
                System.err.println("...but the relationships page may still be usable.");
            }

        	listener.graphingSummaryProgressed();
            html.writeln("</td></tr></table>");
            writeExcludedColumns(excludedColumns, null, html);

            writeFooter(html);
            return true;
        } catch (Dot.DotFailure dotFailure) {
            System.err.println(dotFailure);
            return false;
        } catch (IOException ioExc) {
            ioExc.printStackTrace();
            return false;
        }
    }

    @Override
    protected boolean isRelationshipsPage() {
        return true;
    }
}