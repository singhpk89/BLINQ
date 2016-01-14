package com.blinq.utils;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;

import com.blinq.MeCardHolder;
import com.blinq.PreferencesManager;
import com.blinq.authentication.impl.facebook.FacebookAuthenticator;
import com.blinq.authentication.impl.provider.ServerContactMapper;
import com.blinq.models.Contact;
import com.blinq.models.MemberContact;
import com.blinq.models.Platform;
import com.blinq.models.server.ContactProfile;
import com.blinq.models.server.SocialContactProfile;
import com.blinq.models.social.window.MeCard;
import com.blinq.provider.ContactsMerger;
import com.blinq.provider.FeedProvider;
import com.blinq.provider.FeedProviderImpl;
import com.blinq.provider.Provider;
import com.blinq.server.BlinqRestClientUsage;
import com.blinq.server.BlinqRestClientUsage.ServerRequestCallbacks;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ServerUtils implements ServerRequestCallbacks {

    private static final String TAG = ServerUtils.class.getSimpleName();

    private static ServerUtils instance;
    private final Context context;
    private final PreferencesManager preferencesManager;
    private OnGetContactInformationListener listener;
    private final BlinqRestClientUsage clientUsage;

    private MeCardHolder meCardHolder;
    private long feedId;
    Map<Platform, List<MemberContact>> contacts;

    public static ServerUtils getInstance(Context context) {
        if (instance == null) {
            instance = new ServerUtils(context);
        }
        return instance;
    }

    private ServerUtils(Context context) {
        this.clientUsage = new BlinqRestClientUsage(context, this);
        this.context = context;
        this.preferencesManager = new PreferencesManager(context);
        this.meCardHolder = MeCardHolder.getInstance();
    }

    public void sendContactsDatabase() {
        clientUsage.sendContactsDatabase(context);
    }

    public void getContactInformation(Map<Platform, List<MemberContact>> contacts, long feedId,
                                      OnGetContactInformationListener listener) {
        this.listener = listener;
        this.feedId = feedId;
        this.contacts = contacts;
        mutualFriendsTask.execute();
        clientUsage.getContactInformation(contacts, feedId);
    }

    @Override
    public void getServerContactCompleted(ContactProfile contactProfile, Map<String,String> paramsMap, long feedId) {

        if (contactProfile.getFacebookSocialProfile()!= null &&
                paramsMap.get(Constants.FACEBOOK_PARAM) == null &&
                paramsMap.get(Constants.FACEBOOK_ID_PARAM) == null) {
            updateContactFromServerResponse(contactProfile, Platform.FACEBOOK, feedId);
        }
        if (contactProfile.getInstagramSocialProfile() != null ){
            updateContactFromServerResponse(contactProfile, Platform.INSTAGRAM, feedId);
        }
        if (contactProfile.getTwitterSocialProfile() != null && paramsMap.get(Constants.TWITTER_PARAM) == null){
            updateContactFromServerResponse(contactProfile, Platform.TWITTER, feedId);
        }

        updateContactStaticInfo(contactProfile);

        if (listener != null) {
            listener.onGetContactInformation(true);
        }
    }

    private void updateContactFromServerResponse(ContactProfile contactProfile, Platform platform, long feedId) {
        Provider provider = FeedProviderImpl.getInstance(context);
        Contact contact = ServerContactMapper.convertSocialProfileToContact(contactProfile, platform);
        if (contact != null && contactProfile != null){
            contact.setEmail(contactProfile.getEmail() == null ? "": contactProfile.getEmail());
        }
        Uri uri =  provider.insertContact(contact);
        String id = uri.getLastPathSegment();
        ContactsMerger.getInstance(context)
                .setContacts(provider.getFeed(feedId).getContact().getContactId(),id)
                .setPlatform(platform)
                .merge();
    }

    private void updateContactStaticInfo(ContactProfile contactProfile) {
        Contact contact = ServerContactMapper.convertStaticInfo(contactProfile);
        if (contact != null && contactProfile != null){
            contact.setEmail(contactProfile.getEmail() == null ? "": contactProfile.getEmail());
        }

        //TODO: ugly hack - need to be replaced with db
        updateMeCardData(contactProfile, contact.getBio(),
                contact.getOrganizationName(),
                contact.getTitle(),
                contact.getPhotoUri().toString(),
                contact.getName());
    }

    private String getUsername(SocialContactProfile profile) {
        if(profile == null)
            return null;

        if(profile.getUsername() == null)
            return profile.getId();

        return profile.getUsername();
    }

    private void updateMeCardData(ContactProfile contactProfile, String bio, String organizationName,
                                  String title, String photoUri, String name) {

        if (contactProfile != null) {
            meCardHolder.setSocialProfile(Platform.ANGELLIST, getUsername(contactProfile.getAngellistSocialProfile()));
            meCardHolder.setSocialProfile(Platform.FOURSQUARE, getUsername(contactProfile.getFoursquareSocialProfile()));
            meCardHolder.setSocialProfile(Platform.GITHUB, getUsername(contactProfile.getGithubSocialProfile()));
            meCardHolder.setSocialProfile(Platform.GOOGLEPLUS, getUsername(contactProfile.getGooglePlusSocialProfile()));
            meCardHolder.setSocialProfile(Platform.GRAVATAR, getUsername(contactProfile.getGravatarSocialProfile()));
            meCardHolder.setSocialProfile(Platform.KLOUT, getUsername(contactProfile.getKloutSocialProfile()));
            meCardHolder.setSocialProfile(Platform.LINKEDIN, getUsername(contactProfile.getLinkedinSocialProfile()));
            meCardHolder.setSocialProfile(Platform.PICASA, getUsername(contactProfile.getPicasaSocialProfile()));
            meCardHolder.setSocialProfile(Platform.PINTEREST, getUsername(contactProfile.getPinterestSocialProfile()));
            meCardHolder.setSocialProfile(Platform.TWITTER, getUsername(contactProfile.getTwitterSocialProfile()));
            meCardHolder.setSocialProfile(Platform.INSTAGRAM, getUsername(contactProfile.getInstagramSocialProfile()));
            meCardHolder.setSocialProfile(Platform.FACEBOOK, getUsername(contactProfile.getFacebookSocialProfile()));
            meCardHolder.setSocialProfile(Platform.VIMEO, getUsername(contactProfile.getVimeoSocialProfile()));
        }

        if (StringUtils.isBlank(meCardHolder.getSocialProfile(Platform.FACEBOOK))) {
            meCardHolder.setSocialProfile(Platform.FACEBOOK, preferencesManager.getLastPersonSocialProfileFromMerge(Platform.FACEBOOK, feedId));
        }
        if (StringUtils.isBlank(meCardHolder.getSocialProfile(Platform.TWITTER))) {
            meCardHolder.setSocialProfile(Platform.TWITTER, preferencesManager.getLastPersonSocialProfileFromMerge(Platform.TWITTER, feedId));
        }
        if (StringUtils.isBlank(meCardHolder.getSocialProfile(Platform.INSTAGRAM))) {
            meCardHolder.setSocialProfile(Platform.INSTAGRAM, preferencesManager.getLastPersonSocialProfileFromMerge(Platform.INSTAGRAM, feedId));
        }

        meCardHolder.setBio(bio);
        meCardHolder.setOrganization(organizationName);
        meCardHolder.setTitle(title);

        meCardHolder.setName(StringUtils.isBlank(name) ? preferencesManager.getLastNotificationContactName() : name);
        meCardHolder.setImagePath(StringUtils.isBlank(photoUri) ? FeedProviderImpl.getInstance(context).getFeed(feedId).getContact().getPhotoUri().toString() : photoUri);

        try {
            meCardHolder.setMutualFriends(mutualFriendsTask.get());
        } catch (Exception e) {
            Log.e(TAG, "Failed to get mutual friends for " + e.getMessage());
        }

        context.getContentResolver().update(Uri.withAppendedPath(FeedProvider.UPDATE_CONTACT_NAME_FROM_SERVER,
                Long.toString(feedId)), null, null, new String[]{name, photoUri});

    }

    AsyncTask<Void, Void, List<MeCard.MutualFriend>> mutualFriendsTask = new AsyncTask<Void, Void, List<MeCard.MutualFriend>>() {

        @Override
        protected List<MeCard.MutualFriend> doInBackground(Void... params) {
            List<MemberContact> contact = contacts.get(Platform.FACEBOOK);
            if (contact == null || contact.isEmpty() || contact.get(0).getId() == null) {
                return new ArrayList<MeCard.MutualFriend>();
            }
            return FacebookAuthenticator.getInstance(context).getMutualFriends(contact.get(0).getId());
        }
    };

    @Override
    public void getServerContactFailed(int statusCode, JSONObject errorResponse, Map<String, String> paramsMap) {
        if (errorResponse != null) {
            Log.e(TAG, errorResponse.toString());
        }

        if (listener != null) {
            listener.onGetContactInformation(false);
        }
    }

    public interface OnGetContactInformationListener {
        void onGetContactInformation(boolean success);
    }


}
