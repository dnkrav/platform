package lsfusion.server.physics.admin.authentication;

import lsfusion.base.BaseUtils;
import lsfusion.server.logics.action.controller.stack.ExecutionStack;
import lsfusion.server.data.DataObject;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.language.action.LA;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.classes.user.AbstractCustomClass;
import lsfusion.server.logics.classes.user.ConcreteCustomClass;
import lsfusion.server.logics.classes.user.set.ResolveClassSet;
import lsfusion.server.logics.property.env.CurrentAuthTokenFormulaProperty;
import lsfusion.server.logics.property.env.CurrentComputerFormulaProperty;
import lsfusion.server.logics.property.env.CurrentUserFormulaProperty;
import org.antlr.runtime.RecognitionException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;

import static lsfusion.base.BaseUtils.nullTrim;

public class AuthenticationLogicsModule extends ScriptingLogicsModule{
    public ConcreteCustomClass computer;
    public AbstractCustomClass user;
    public ConcreteCustomClass systemUser;
    public ConcreteCustomClass customUser;

    public LP firstNameContact;
    public LP lastNameContact;
    public LP emailContact;
    public LP contactEmail;

    public LP isLockedCustomUser;
    public LP isLockedLogin;
    public LP<?> loginCustomUser;
    public LP customUserLogin;
    public LP customUserUpcaseLogin;
    public LP sha256PasswordCustomUser;
    public LP calculatedHash;
    public LP lastActivityCustomUser;
    public LP lastComputerCustomUser;
    public LP currentUser;
    public LP currentUserName;
    public LP nameContact;
    public LP currentUserAllowExcessAllocatedBytes;
    public LP allowExcessAllocatedBytes;

    public LP currentAuthToken;
    public LP secret;

    public LP hostnameComputer;
    public LP computerHostname;
    public LP currentComputer;
    public LP hostnameCurrentComputer;

    public LP minHashLength;
    public LP useLDAP;
    public LP serverLDAP;
    public LP portLDAP;
    public LP baseDNLDAP;
    public LP userDNSuffixLDAP;

    public LP useBusyDialog;
    public LP useRequestTimeout;

    public LP language;
    public LP country;
    public LP timeZone;
    public LP twoDigitYearStart;
    
    public LP clientLanguage;
    public LP clientCountry;

    public LP defaultLanguage;
    public LP defaultCountry;
    public LP defaultTimeZone;
    public LP defaultTwoDigitYearStart;
    
    public LP userFontSize;
    
    public LA deliveredNotificationAction;
    
    public LA<?> syncUsers;

    public AuthenticationLogicsModule(BusinessLogics BL, BaseLogicsModule baseLM) throws IOException {
        super(AuthenticationLogicsModule.class.getResourceAsStream("/system/Authentication.lsf"), "/system/Authentication.lsf", baseLM, BL);
    }

    @Override
    public void initMetaAndClasses() throws RecognitionException {
        super.initMetaAndClasses();

        computer = (ConcreteCustomClass) findClass("Computer");
        user = (AbstractCustomClass) findClass("User");
        systemUser = (ConcreteCustomClass) findClass("SystemUser");
        customUser = (ConcreteCustomClass) findClass("CustomUser");
    }

    @Override
    public void initMainLogic() throws RecognitionException {
        // Текущий пользователь
        currentUser = addProperty(null, new LP<>(new CurrentUserFormulaProperty(user)));
        makePropertyPublic(currentUser, "currentUser", new ArrayList<ResolveClassSet>());
        currentComputer = addProperty(null, new LP<>(new CurrentComputerFormulaProperty(computer)));
        makePropertyPublic(currentComputer, "currentComputer", new ArrayList<ResolveClassSet>());
        currentAuthToken = addProperty(null, new LP<>(new CurrentAuthTokenFormulaProperty()));
        makePropertyPublic(currentAuthToken, "currentAuthToken", new ArrayList<ResolveClassSet>());

        super.initMainLogic();

        firstNameContact = findProperty("firstName[Contact]");
        lastNameContact = findProperty("lastName[Contact]");
        emailContact = findProperty("email[Contact]");
        contactEmail = findProperty("contact[VARSTRING[400]]");

        nameContact = findProperty("name[Contact]");
        currentUserName = findProperty("currentUserName[]");
        allowExcessAllocatedBytes = findProperty("allowExcessAllocatedBytes[CustomUser]");
        currentUserAllowExcessAllocatedBytes = findProperty("currentUserAllowExcessAllocatedBytes[]");

        // Компьютер
        hostnameComputer = findProperty("hostname[Computer]");
        computerHostname = findProperty("computer[VARISTRING[100]]");
        hostnameCurrentComputer = findProperty("hostnameCurrentComputer[]");

        isLockedCustomUser = findProperty("isLocked[CustomUser]");
        isLockedLogin = findProperty("isLockedLogin[STRING[100]]");

        loginCustomUser = findProperty("login[CustomUser]");
        customUserLogin = findProperty("customUser[STRING[100]]");
        customUserUpcaseLogin = findProperty("customUserUpcase[?]");

        sha256PasswordCustomUser = findProperty("sha256Password[CustomUser]");
        sha256PasswordCustomUser.setEchoSymbols(true);

        calculatedHash = findProperty("calculatedHash[]");

        lastActivityCustomUser = findProperty("lastActivity[CustomUser]");
        lastComputerCustomUser = findProperty("lastComputer[CustomUser]");

        secret = findProperty("secret[]");

        minHashLength = findProperty("minHashLength[]");
        useLDAP = findProperty("useLDAP[]");
        serverLDAP = findProperty("serverLDAP[]");
        portLDAP = findProperty("portLDAP[]");
        baseDNLDAP = findProperty("baseDNLDAP[]");
        userDNSuffixLDAP = findProperty("userDNSuffixLDAP[]");

        useBusyDialog = findProperty("useBusyDialog[]");
        useRequestTimeout = findProperty("useRequestTimeout[]");

        language = findProperty("language[CustomUser]");
        country = findProperty("country[CustomUser]");
        timeZone = findProperty("timeZone[CustomUser]");
        twoDigitYearStart = findProperty("twoDigitYearStart[CustomUser]");
        
        clientCountry = findProperty("clientCountry[CustomUser]");
        clientLanguage = findProperty("clientLanguage[CustomUser]");

        defaultLanguage = findProperty("defaultUserLanguage[]");
        defaultCountry = findProperty("defaultUserCountry[]");
        defaultTimeZone = findProperty("defaultUserTimeZone[]");
        defaultTwoDigitYearStart = findProperty("defaultUserTwoDigitYearStart[]");
        
        userFontSize = findProperty("fontSize[CustomUser]");
        
        deliveredNotificationAction = findAction("deliveredNotificationAction[CustomUser]");
        
        syncUsers = findAction("syncUsers[VARISTRING[100], JSONFILE]");
    }
    
    public boolean checkPassword(DataSession session, DataObject userObject, String password, ExecutionStack stack) throws SQLException, SQLHandledException {
        boolean authenticated = true;
        String hashPassword = (String) sha256PasswordCustomUser.read(session, userObject);
        String newHashInput = BaseUtils.calculateBase64Hash("SHA-256", nullTrim(password), UserInfo.salt);
        if (hashPassword == null || !hashPassword.trim().equals(newHashInput)) {
            //TODO: убрать, когда будем считать, что хэши у всех паролей уже перебиты
            Integer minHashLengthValue = (Integer) minHashLength.read(session);
            String oldHashInput = BaseUtils.calculateBase64HashOld("SHA-256", nullTrim(password), UserInfo.salt);
            if (minHashLengthValue == null)
                minHashLengthValue = oldHashInput.length();
            //если совпали первые n символов, считаем пароль правильным и сохраняем новый хэш в базу
            if (hashPassword != null &&
                    hashPassword.trim().substring(0, Math.min(hashPassword.trim().length(), minHashLengthValue)).equals(oldHashInput.substring(0, Math.min(oldHashInput.length(), minHashLengthValue)))) {
                sha256PasswordCustomUser.change(newHashInput, session, userObject);
                session.applyException(BL, stack);
            } else {
                authenticated = false;
            }
        }
        return authenticated;
    }
}