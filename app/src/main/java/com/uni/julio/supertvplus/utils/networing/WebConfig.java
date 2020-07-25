package com.uni.julio.supertvplus.utils.networing;

public class WebConfig {

    private static final String domain = "https://supertvultra.com/";
    public static final String baseURL              = "https://supertvultra.com/Wm7TSJNObuIB1Y1G/";
    public static final String GetCodeURL = "https://supertvultra.com/dtYjAcIdG7NYBhITjmA/Connect/loginCode.php?request_code";
    public static final String getCategoriesForYear = "https://supertvultra.com/dtYjAcIdG7NYBhITjmA/Connect/movie_category_year.php";
    public static final String LoginSplash = "https://supertvultra.com/dtYjAcIdG7NYBhITjmA/Connect/signin.php?user={USER}&pass={PASS}&istv={ISTV}&device_id={DEVICE_ID}&movie={MOVIE}&splash";
    public static final String getMessage = "https://supertvultra.com/dtYjAcIdG7NYBhITjmA/Connect/getMessage.php?user={USER}";
    public static final String removeUserURL = "https://supertvultra.com/dtYjAcIdG7NYBhITjmA/Connect/signin.php?user={USER}&device_num=0&device_id={DEVICE_ID}&delete";
    public static final String likeURL = "https://supertvultra.com/dtYjAcIdG7NYBhITjmA/Connect/like.php?movieId={MOVIEID}&like={LIKE}&dislike={DISLIKE}&userId={USERID}";
    public static final String getLikeURL = "https://supertvultra.com/dtYjAcIdG7NYBhITjmA/Connect/getLike.php?movieId={MOVIEID}&userId={USERID}";
    public static final String loginURL             = domain + "/dtYjAcIdG7NYBhITjmA/Connect/signin.php?user={USER}&pass={PASS}&device_id={DEVICE_ID}&model={MODEL}&fw={FW}&country={COUNTRY}&istv={ISTV}";
    public static final String signUpURL             = domain + "/dtYjAcIdG7NYBhITjmA/Connect/signup.php?email={EMAIL}&user={USER}&pass={PASS}&device_id={DEVICE_ID}&model={MODEL}&fw={FW}&country={COUNTRY}";
    public static final String sendVerificationCode             = domain + "/dtYjAcIdG7NYBhITjmA/Connect/email_verify.php?email={EMAIL}";
    public static final String liveTVCategoriesURL          = domain + "dtYjAcIdG7NYBhITjmA/Connect/live_categorias.php";
    public static final String liveTVChannelsURL            = domain + "/dtYjAcIdG7NYBhITjmA/Connect/live_canales.php?cve={CAT_ID}";
    public static final String updateURL = "https://supertvultra.com/dtYjAcIdG7NYBhITjmA/Connect/upgrade_version.php?new_version";
    public static final String videoSearchURL = "https://supertvultra.com/dtYjAcIdG7NYBhITjmA/Connect/searchVideo.php?type={TYPE}&pattern={PATTERN}";
    public static final String getMoviesYear = "https://supertvultra.com/dtYjAcIdG7NYBhITjmA/Connect/movie_year.php?year={YEAR}";
    public static final String reportUrl = baseURL + "reportar.php?cve={CVE}&tipo={TIPO}&user={USER}&act={ACT}";
    public static final String orderUrl = baseURL + "order_title.php?tipo={TIPO}&user={USER}&titulo={TITLE}";
    public static final String update_subscriptionURl = "https://supertvultra.com/dtYjAcIdG7NYBhITjmA/Connect/update_subscription.php?email={EMAIL}&expiration_date={EXPIRATION_DATE}&sku={SKU}&purchaseToken={TOKEN}";

}
