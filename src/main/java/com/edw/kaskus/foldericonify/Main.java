package com.edw.kaskus.foldericonify;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import net.sf.image4j.codec.ico.ICOEncoder;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

/**
 * <pre>
 *  com.edw.kaskus.foldericonify.Main
 * </pre>
 *
 * @author edwin < edwinkun at gmail dot com >
 * Feb 5, 2016 1:51:16 AM
 *
 */
public class Main {

    /**
     * desktop.ini content
     */
    private static final String desktopIni = "[.ShellClassInfo]\n"
            + "ConfirmFileOp=0\n"
            + "NoSharing=1\n"
            + "IconFile=folder.ico\n"
            + "IconIndex=0\n"
            + "InfoTip=Some sensible information.\n"
            + "IconResource=folder.ico";

    /**
     * google cse search parameter
     *
     * @see      
     * <pre>
     *      https://developers.google.com/apis-explorer/#s/customsearch/v1/search.cse.list
     * </pre>
     */
    private static final String GOOGLE_URL = "https://www.googleapis.com/customsearch/v1?q=QUERY&fileType=jpg&imgSize=medium&searchType=image&key=KEY&cx=CX&num=1";

    /**
     *  your google API KEY
     */
    private static final String KEY = "???";

    /**
     *  your google cse id
     */
    private static final String CX = "???";
    
    /**
     *  your movie folder location, dont forget to use double backslash for win
     */
    private static final String FOLDER_PATH = "C:\\Users\\my\\Videos";

    private static final Logger logger = Logger.getLogger(Main.class);

    /**
     * everything starts here
     *
     * @param args
     */
    public static void main(String[] args) {
        File folder = new File(FOLDER_PATH);
        for (File file : folder.listFiles()) {
            if (file.isDirectory()) {
                try {
                    // create desktop.ini
                    File desktopDotIni = new File(file.getAbsoluteFile() + "\\desktop.ini");
                    File folderIco = new File(file.getAbsoluteFile() + "\\folder.ico");
                    FileUtils.write(desktopDotIni, desktopIni, "UTF-8");

                    // grabbing from google
                    HttpClient httpclient = HttpClientBuilder.create().build();
                    String folderSearchUrl = (GOOGLE_URL.replace("KEY", KEY).replace("CX", CX).replace("QUERY", URLEncoder.encode(file.getName() + " POSTER", "UTF-8")));
                    HttpGet httpGet = new HttpGet(folderSearchUrl);
                    HttpResponse response = httpclient.execute(httpGet);
                    HttpEntity resEntity = response.getEntity();

                    String responseText = EntityUtils.toString(resEntity);
                    logger.debug("google json response : " + responseText);

                    // reading the content
                    HashMap<String, Object> result = new ObjectMapper().readValue(responseText, HashMap.class);
                    String imageLink = (String) ((List<Map>) result.get("items")).get(0).get("link");

                    logger.debug("image link : " + imageLink);

                    // download image as folder.jpg
                    httpGet = new HttpGet(imageLink);
                    response = httpclient.execute(httpGet);
                    resEntity = response.getEntity();

                    BufferedInputStream bis = new BufferedInputStream(resEntity.getContent());
                    BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(new File(file.getAbsoluteFile() + "\\folder.jpg")));

                    int inByte;
                    while ((inByte = bis.read()) != -1) {
                        bos.write(inByte);
                    }
                    bis.close();
                    bos.close();

                    // generate icon file
                    BufferedImage bufferedImage = ImageIO.read(new File(file.getAbsoluteFile() + "\\folder.jpg"));
                    BufferedImage resizedImage = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g = resizedImage.createGraphics();

                    g.setComposite(AlphaComposite.Src);
                    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    g.drawImage(bufferedImage, 25, 0, 200, 256, null);
                    g.dispose();

                    List<BufferedImage> bufferedImages = new ArrayList<>();
                    bufferedImages.add(resizedImage);

                    ICOEncoder.write(bufferedImages, folderIco);

                    // finally, attrib the folder 
                    Runtime rt = Runtime.getRuntime();
                    rt.exec("attrib +S +H \"" + folderIco.getAbsoluteFile() + "\"");
                    rt.exec("attrib +S \"" + file.getAbsoluteFile() + "\"");
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
    }
}
