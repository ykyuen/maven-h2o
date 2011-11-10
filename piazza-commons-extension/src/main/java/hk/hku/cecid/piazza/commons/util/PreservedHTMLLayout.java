/* 
 * Copyright(c) 2005 Center for E-Commerce Infrastructure Development, The
 * University of Hong Kong (HKU). All Rights Reserved.
 *
 * This software is licensed under the GNU GENERAL PUBLIC LICENSE Version 2.0 [1]
 * 
 * [1] http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt
 */

package hk.hku.cecid.piazza.commons.util;

import org.apache.log4j.helpers.Transform;
import org.apache.log4j.Level;
import org.apache.log4j.Layout;
import org.apache.log4j.HTMLLayout;
import org.apache.log4j.spi.LoggingEvent;

public class PreservedHTMLLayout extends HTMLLayout {

	public String format(LoggingEvent event){
		StringBuffer sbuf = new StringBuffer(BUF_SIZE);
		if (sbuf.capacity() > MAX_CAPACITY) {
			sbuf = new StringBuffer(BUF_SIZE);
		} else {
			sbuf.setLength(0);
		}

		sbuf.append(Layout.LINE_SEP + "<tr>" + Layout.LINE_SEP);

		sbuf.append("<td rowspan=\"3\" width=\"5%\">");
		sbuf.append(event.timeStamp - event.getStartTime());
		sbuf.append("</td>" + Layout.LINE_SEP);

		sbuf.append("<td rowspan=\"3\" width=\"5%\" title=\"" + event.getThreadName() + " thread\">");
		sbuf.append(Transform.escapeTags(event.getThreadName()));
		sbuf.append("</td>" + Layout.LINE_SEP);

		sbuf.append("<td rowspan=\"3\" width=\"5%\" title=\"Level\">");
		if (event.getLevel().equals(Level.DEBUG)) {
			sbuf.append("<font color=\"#339933\">");
			sbuf.append(event.getLevel());
			sbuf.append("</font>");
		} else if (event.getLevel().isGreaterOrEqual(Level.WARN)) {
			sbuf.append("<font color=\"#993300\"><strong>");
			sbuf.append(event.getLevel());
			sbuf.append("</strong></font>");
		} else {
			sbuf.append(event.getLevel());
		}
		sbuf.append("</td>" + Layout.LINE_SEP);
			
		sbuf.append("<tr><td width=\"85%\" title=\"" + event.getLoggerName() + " category\">");
		sbuf.append(Transform.escapeTags(event.getLoggerName()));
		sbuf.append("</td></tr>" + Layout.LINE_SEP);
		
		sbuf.append("<tr><td width=\"85%\" title=\"Message\"><div><pre>");
		sbuf.append(Transform.escapeTags(event.getRenderedMessage()));
		sbuf.append("<pre></div></td>" + Layout.LINE_SEP);
		sbuf.append("</tr>" + Layout.LINE_SEP);

		if (event.getNDC() != null) {
			sbuf.append("<tr><td bgcolor=\"#EEEEEE\" style=\"font-size : xx-small;\" colspan=\"6\" title=\"Nested Diagnostic Context\">");
			sbuf.append("NDC: " + Transform.escapeTags(event.getNDC()));
			sbuf.append("</td></tr>" + Layout.LINE_SEP);
		}

		String[] s = event.getThrowableStrRep();
		if (s != null) {
			sbuf.append("<tr><td bgcolor=\"#993300\" style=\"color:White; font-size : xx-small;\" colspan=\"6\">");
			if (s != null) {
				int len = s.length;
				if (len != 0){ 
					sbuf.append(Transform.escapeTags(s[0]));
					sbuf.append(Layout.LINE_SEP);
					for (int i = 1; i < len; i++) {
						sbuf.append("<br>&nbsp;&nbsp;&nbsp;&nbsp;");
						sbuf.append(Transform.escapeTags(s[i]));
						sbuf.append(Layout.LINE_SEP);
					}
				}
			}
			sbuf.append("</td></tr>" + Layout.LINE_SEP);
		}
		return sbuf.toString();
	}
	
	
	public StringBuffer getCSS(StringBuffer sbuf){
		if (sbuf != null){
			sbuf.append("<style type=\"text/css\">"  + Layout.LINE_SEP);
		    sbuf.append("<!--"  + Layout.LINE_SEP);
		    sbuf.append("body, table {font-family: arial,sans-serif; font-size: x-small;}" + Layout.LINE_SEP);
		    sbuf.append("th {background: #336699; color: #FFFFFF; text-align: left;}" + Layout.LINE_SEP);
		    sbuf.append("div {overflow: auto;}" + Layout.LINE_SEP);
		    sbuf.append("pre {font-size: medium; line-height: 1.0em;}" + Layout.LINE_SEP);
		    sbuf.append("-->" + Layout.LINE_SEP);
		    sbuf.append("</style>" + Layout.LINE_SEP);
		}
		return sbuf;
	}
	
	public StringBuffer getTable(StringBuffer sbuf){
		if (sbuf == null) return sbuf;
		
		sbuf.append("<table cellspacing=\"0\" cellpadding=\"4\" border=\"1\" bordercolor=\"#224466\" width=\"100%\">" + Layout.LINE_SEP);
		sbuf.append("<tr>" + Layout.LINE_SEP);
		sbuf.append("<th>Time</th>" + Layout.LINE_SEP);
		sbuf.append("<th>Thread</th>" + Layout.LINE_SEP);
		sbuf.append("<th>Level</th>" + Layout.LINE_SEP);
		sbuf.append("<th>Category / Message </th>" + Layout.LINE_SEP);
		sbuf.append("</tr>" + Layout.LINE_SEP);
		return sbuf;
	}

	/* (non-Javadoc)
	 * @see org.apache.log4j.HTMLLayout#getHeader()
	 */
	public String getHeader() {
		StringBuffer sbuf = new StringBuffer();
		
		sbuf.append("<head>" + Layout.LINE_SEP);
		sbuf = this.getCSS(sbuf);		
	    sbuf.append("</head>" + Layout.LINE_SEP);
	    sbuf = this.getTable(sbuf);
	    
		return sbuf.toString();
	}		
}
