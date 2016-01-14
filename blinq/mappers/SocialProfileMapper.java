package com.blinq.mappers;

import com.blinq.MeCardHolder;
import com.blinq.R;
import com.blinq.models.Platform;
import com.blinq.models.social.window.MeCard;
import com.blinq.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Maps social profiles from the preferences manager
 * to memory.
 *
 * Social profiles includes: github, angellist, linkedin
 * and others - Used mainly in the me card
 *
 * Created by galbracha on 12/24/14.
 */
public class SocialProfileMapper {

    private static SocialProfileMapper instance;
    private final MeCardHolder meCardHolder;
    private List<MeCard.SocialProfile> results;

    private SocialProfileMapper() {
        this.meCardHolder = MeCardHolder.getInstance();
    }

    public static SocialProfileMapper getInstance() {
        if (instance == null) {
            instance = new SocialProfileMapper();
        }
        return instance;
    }

    private void addSocialProfile(String username, String urlTemplate, int resourcesId) {
        if(!StringUtils.isBlank(username)) {
            results.add(new MeCard.SocialProfile(String.format(urlTemplate, username), resourcesId));
        }
    }

    public List<MeCard.SocialProfile> map() {
        results = new ArrayList<MeCard.SocialProfile>();
        String socialProfileLinkedin = meCardHolder.getSocialProfile(Platform.LINKEDIN);
        addSocialProfile(socialProfileLinkedin, "https://linkedin.com/in/%s", R.drawable.linkedin);
        String socialProfileAngellist = meCardHolder.getSocialProfile(Platform.ANGELLIST);;
        addSocialProfile(socialProfileAngellist, "https://angel.co/%s", R.drawable.angelist);
        String socialProfileFoursquare = meCardHolder.getSocialProfile(Platform.FOURSQUARE);
        addSocialProfile(socialProfileFoursquare, "https://foursquare.com/%s", R.drawable.foursquare);
        String socialProfileGithub = meCardHolder.getSocialProfile(Platform.GITHUB);
        addSocialProfile(socialProfileGithub, "https://github.com/%s", R.drawable.github);
        String socialProfileGooglePlus = meCardHolder.getSocialProfile(Platform.GOOGLEPLUS);
        addSocialProfile(socialProfileGooglePlus, "https://plus.google.com/%s", R.drawable.gplus);
        String socialProfilePinterest = meCardHolder.getSocialProfile(Platform.PINTEREST);
        addSocialProfile(socialProfilePinterest, "https://pinterest.com/%s", R.drawable.pinterest);
        String socialProfileKlout = meCardHolder.getSocialProfile(Platform.KLOUT);
        addSocialProfile(socialProfileKlout, "https://klout.com/%s", R.drawable.klout);
        String socialProfileVimeo = meCardHolder.getSocialProfile(Platform.VIMEO);
        addSocialProfile(socialProfileVimeo, "https://vimeo.com/%s", R.drawable.vimeo);
        String socialProfileAboutMe = meCardHolder.getSocialProfile(Platform.ABOUTME);
        addSocialProfile(socialProfileAboutMe, "https://about.me/%s", R.drawable.aboutme);
        String socialProfileTwitter = meCardHolder.getSocialProfile(Platform.TWITTER);
        addSocialProfile(socialProfileTwitter, "https://twitter.com/%s", R.drawable.twitter);
        String socialProfileInstagram = meCardHolder.getSocialProfile(Platform.INSTAGRAM);
        addSocialProfile(socialProfileInstagram, "https://instagram.com/%s", R.drawable.instagram);
        String socialProfileGravatar = meCardHolder.getSocialProfile(Platform.GRAVATAR);
        addSocialProfile(socialProfileGravatar, "https://gravatar.com/%s", R.drawable.gravatar);

        addFacebookOnlyIfMoreExists();

        //TODO: not sure what is the template for picasa urls - https://picasa.com/%s ??
        //String socialProfilePicasa = preferencesManager.getLastPersonSocialProfilePicasa();
        //addSocialProfile(socialProfilePicasa, "https://picasa.com/%s", R.drawable.twitter);
        return results;
    }

    // means don't show the me card if only facebook profile
    private void addFacebookOnlyIfMoreExists() {
        String socialProfileFacebook = meCardHolder.getSocialProfile(Platform.FACEBOOK);
        if(!results.isEmpty())
            addSocialProfile(socialProfileFacebook, "https://facebook.com/%s", R.drawable.facebook);
    }

}
