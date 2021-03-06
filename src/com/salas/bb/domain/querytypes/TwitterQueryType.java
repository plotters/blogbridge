// BlogBridge -- RSS feed reader, manager, and web based service
// Copyright (C) 2002-2006 by R. Pito Salas
//
// This program is free software; you can redistribute it and/or modify it under
// the terms of the GNU General Public License as published by the Free Software Foundation;
// either version 2 of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
// without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
// See the GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License along with this program;
// if not, write to the Free Software Foundation, Inc., 59 Temple Place,
// Suite 330, Boston, MA 02111-1307 USA
//
// Contact: R. Pito Salas
// mailto:pitosalas@users.sourceforge.net
// More information: about BlogBridge
// http://www.blogbridge.com
// http://sourceforge.net/projects/blogbridge
//
// $Id: DiggQueryType.java,v 1.2 2006/12/11 13:54:37 spyromus Exp $
//

package com.salas.bb.domain.querytypes;

import com.salas.bb.core.GlobalController;
import com.salas.bb.domain.FeedType;
import com.salas.bb.domain.IArticle;
import com.salas.bb.domain.QueryFeed;
import com.salas.bb.domain.StandardArticle;
import com.salas.bb.twitter.TwitterFeature;
import com.salas.bb.twitter.TwitterGateway;
import com.salas.bb.twitter.TwitterPreferences;
import com.salas.bb.utils.Constants;
import com.salas.bb.utils.ResourceID;
import com.salas.bb.utils.StringUtils;
import com.salas.bb.utils.i18n.Strings;
import com.salas.bb.utils.net.BBHttpClient;
import com.salas.bb.utils.parser.*;
import com.salas.bb.utils.uif.BBFormBuilder;
import com.salas.bb.utils.uif.UifUtilities;
import com.salas.bb.views.feeds.IFeedDisplayConstants;
import oauth.signpost.exception.OAuthException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Twitter query type.
 */
class TwitterQueryType extends DefaultQueryType
{
    private static final Logger LOG = Logger.getLogger(TwitterQueryType.class.getName());

    private static final Pattern PTN_LIST = Pattern.compile("^@([^\\s/]+)/([^\\s/]+)$");
    private static final String PATTERN_QUERY   = "http://search.twitter.com/search.atom?q={0}&rpp={1}";
    private static final String PATTERN_FRIENDS = "http://{0}:{1}@twitter.com/statuses/friends_timeline/{0}.rss";

    /**
     * Creates name query type with associated icon and URL pattern. The pattern can use the
     * formatting rules of <code>MessageFormat</code> class and keep in mind that:
     * <ul>
     * <li><b>{0]</b> is a list of parameters, separated with "+" and properly escaped for
     * use in URL.</li>
     * <li><b>{1}</b> maximum number of articles to fetch.</li>
     * </ul>
     *
     * @throws NullPointerException if <code>aName</code> or <code>anURLPattern</code>
     *                              is not specified.
     */
    protected TwitterQueryType()
    {
        super(TYPE_TWITTER, FeedType.TWITTER,
            Strings.message("queryfeed.type.twitter.name"),
            ResourceID.ICON_QUERY_FEED_TWITTER,
            "",
            Strings.message("queryfeed.type.twitter.parameter"),
            "",
            Strings.message("queryfeed.type.twitter.description"),
            IFeedDisplayConstants.MODE_FULL);
    }

    @Override
    public IArticle beforeInsertArticle(int index, IArticle article)
    {
        String html = article.getHtmlText();

        // Remove tags
        html = html.replaceAll("<.*?>", "");
        html = html.replaceAll("&apos;", "'");

//        if (article.getPlainText().indexOf("http://") == -1)
//        {
//            return article;
//        } else
//        {
//            // Mobilize items
//            java.util.List<String> links = StringUtils.collectLinks(article.getPlainText());
//            for (String link : links)
//            {
//                try
//                {
//                    LOG.warning("Mobilizing: " + link);
//                    String story = ReadItLater.mobilize(link);
//
//                    LOG.warning("---\n" + story + "\n---");
//
//                    html += story;
//                } catch (IOException e)
//                {
//                    LOG.warning("Failed to mobilze '" + link + "': " + e.getMessage());
//                    e.printStackTrace();
//                }
//            }
//        }

        StandardArticle newArticle = new StandardArticle(html);
        newArticle.setAuthor(article.getAuthor());
        newArticle.setFeed(article.getFeed());
        newArticle.setID(article.getID());
        newArticle.setLink(article.getLink());
        newArticle.setPublicationDate(article.getPublicationDate());
        newArticle.setTitle(article.getTitle());

        return newArticle;
    }

    @Override
    public Channel fetchFeed(QueryFeed queryFeed)
        throws IOException
    {
        Channel res;

        String parameter = queryFeed.getParameter();
        if (parameter.equals("~~"))
        {
            res = fetchFriendsTimeline();
        } else
        {
            res = fetchUserList(parameter);
        }

        return res;
    }

    private static Channel fetchFriendsTimeline()
        throws IOException
    {
        Channel res = null;

        String feedXml = null;
        try
        {
            feedXml = TwitterGateway.friendsTimeline();
        } catch (OAuthException e)
        {
            LOG.warning(MessageFormat.format(Strings.error("feed.fetching.errored"), e.toString()));
            LOG.log(Level.FINE, "Details:", e);
        }

        if (feedXml != null)
        {
            IFeedParser parser = FeedParserConfig.create();

            FeedParserResult result = null;
            try
            {
                result = parser.parse(new ByteArrayInputStream(feedXml.getBytes()), null);
            } catch (FeedParserException e)
            {
                LOG.warning(MessageFormat.format(Strings.error("feed.fetching.errored"), e.toString()));
                LOG.log(Level.FINE, "Details:", e);
            }

            // We either return the result or an empty channel not to let the app try the default parser
            if (result != null) res = result.getChannel();
        }

        return res == null ? new Channel() : res;
    }

    private static Channel fetchUserList(String parameter)
        throws IOException
    {
        Channel res = null;

        Matcher matcher = PTN_LIST.matcher(parameter);
        if (matcher.matches())
        {
            String json = BBHttpClient.get("https://api.twitter.com/1/lists/statuses.json?slug=" +
                matcher.group(2) + "&owner_screen_name=" + matcher.group(1));

            try
            {
                res = new Channel();
                res.setAuthor("Twitter");
                res.setDescription("" + matcher.group(1) + "'s list: " + matcher.group(2));
                res.setFormat("XML");
                res.setLanguage("en_US");
                res.setUpdatePeriod(Constants.MILLIS_IN_DAY);

                JSONArray a = new JSONArray(json);
                for (int i = 0; i < a.length(); i++)
                {
                    JSONObject o = a.getJSONObject(i);

                    JSONObject u = o.getJSONObject("user");

                    Item item = new Item(o.getString("text"));
                    item.setTitle(o.getString("text"));
                    item.setAuthor(u.getString("screen_name"));
                    item.setLink(new URL("http://twitter.com/" + u.getString("screen_name") + "/status/" + o.getString("id")));
                    res.addItem(item);
                }
            } catch (JSONException e)
            {
                throw new RuntimeException(e);
            }
        }
        return res;
    }

    @Override
    protected String formURLString(String param, int limit)
    {
        String url;

        if ("~~".equals(param))
        {
            if (!TwitterFeature.isConfigured()) return null;

            TwitterPreferences tp = GlobalController.SINGLETON.getModel().getUserPreferences().getTwitterPreferences();
            String username = tp.getScreenName();
            String password = "todo"; //tp.getPassword();

            url = MessageFormat.format(PATTERN_FRIENDS, username, password);
        } else
        {
            param = parameterToURLPart(param);
            if (StringUtils.isEmpty(param)) return null;
            url = MessageFormat.format(PATTERN_QUERY, param, limit);
        }

        return url;
    }

    @Override
    public QueryEditorPanel getEditorPanel(int labelColWidth)
    {
        return new QueryEditor(labelColWidth);
    }

    /**
     * Microtag query editor.
     */
    private static class QueryEditor extends QueryEditorPanel
    {
        private static final int    FRIENDS         = 0;
        private static final int    QUERY           = 1;
        private static final int    LIST            = 2;

        private static final String TYPE_FRIENDS    = "Friends Timeline";
        private static final String TYPE_QUERY      = "Query";
        private static final String TYPE_LIST       = "List";

        private final JComboBox  cbType;
        private final JPanel     pnlQuery;
        private final JTextField tfQuery;
        private final JPanel     pnlList;
        private final JTextField tfListUsername;
        private final JTextField tfListName;
        private final JPanel     pnlHolder;
        private final JPanel     pnlAuthWarning;

        private JPanel currentPanel;

        /**
         * Creates the editor.
         *
         * @param labelColWidth the width in 'dlu' of the label column.
         */
        private QueryEditor(int labelColWidth)
        {
            cbType          = new JComboBox();
            tfQuery         = new JTextField();
            tfListUsername  = new JTextField();
            tfListName      = new JTextField();

            // TODO: i18n
            JLabel lblAuthWarning = new JLabel("Enable Twitter support in Preferences to make this feed type work.");
            UifUtilities.smallerFont(lblAuthWarning);

            pnlHolder = new JPanel(new BorderLayout());

            // Initialize types
            cbType.addItem(TYPE_FRIENDS);
            cbType.addItem(TYPE_QUERY);
            cbType.addItem(TYPE_LIST);

            BBFormBuilder b = new BBFormBuilder(labelColWidth + "dlu, 4dlu, p:grow");
            b.append("Query:", tfQuery);
            pnlQuery = b.getPanel();

            b = new BBFormBuilder(labelColWidth + "dlu, 4dlu, 50dlu, 4dlu, p, 4dlu, p:grow");
            b.append("User:", tfListUsername);
            b.append("List:", tfListName);
            pnlList = b.getPanel();

            b = new BBFormBuilder((labelColWidth + "dlu, 4dlu, p, p:grow"));
            b.append("", lblAuthWarning);
            pnlAuthWarning = b.getPanel();

            b = new BBFormBuilder(labelColWidth + "dlu, 4dlu, p, p:grow", this);
            b.append("Type:", cbType);
            b.append(pnlHolder, 4);

            cbType.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    updateFieldState();
                }
            });

            updateFieldState();
        }

        /**
         * Updates the state of fields.
         */
        private void updateFieldState()
        {
            JPanel newPanel;

            switch (cbType.getSelectedIndex()) {
                case QUERY:
                    newPanel = pnlQuery;
                    break;
                case LIST:
                    newPanel = pnlList;
                    break;
                default:
                    newPanel = TwitterFeature.isConfigured() ? null : pnlAuthWarning;
            }

            if (currentPanel != null && currentPanel != newPanel) {
                pnlHolder.remove(currentPanel);
            }

            if (newPanel != null && newPanel != currentPanel) {
                pnlHolder.add(newPanel, BorderLayout.CENTER);
            }

            currentPanel = newPanel;
            pnlHolder.revalidate();
        }

        /**
         * Sets the value of the parameter. Initializes the internal controls with
         * the values deciphered from the parameter given.
         *
         * @param text the text.
         */
        public void setParameter(String text)
        {
            int type;
            String query        = null;
            String listUsername = null;
            String listName     = null;

            Matcher matcher = PTN_LIST.matcher(text);

            if ("~~".equals(text))
            {
                type = FRIENDS;
            } else if (matcher.matches()) {
                type = LIST;
                listUsername = matcher.group(1);
                listName = matcher.group(2);
            } else
            {
                type = QUERY;
                query = text;
            }

            cbType.setSelectedIndex(type);
            tfQuery.setText(query);
            tfListUsername.setText(listUsername);
            tfListName.setText(listName);
        }

        /**
         * Returns the value of the parameter.
         *
         * @return the text.
         */
        public String getParameter()
        {
            String res;

            switch (cbType.getSelectedIndex()) {
                case FRIENDS:
                    res = "~~";
                    break;
                case LIST:
                    res = "@" + tfListUsername.getText().replaceFirst("^@+", "") + "/" + tfListName.getText().replaceFirst("^/+", "");
                    break;
                default:
                    res = tfQuery.getText();
                    break;
            }

            return res;
        }
    }
}