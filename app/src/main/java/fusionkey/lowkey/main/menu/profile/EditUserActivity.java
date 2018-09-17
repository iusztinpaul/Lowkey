package fusionkey.lowkey.main.menu.profile;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;
import fusionkey.lowkey.LowKeyApplication;
import fusionkey.lowkey.R;
import fusionkey.lowkey.auth.utils.AttributesValidator;
import fusionkey.lowkey.auth.utils.AuthCallback;
import fusionkey.lowkey.auth.utils.UserAttributesEnum;
import fusionkey.lowkey.main.Main2Activity;
import fusionkey.lowkey.main.utils.Callback;
import fusionkey.lowkey.main.utils.ProfilePhotoUploader;

public class EditUserActivity extends AppCompatActivity {

    private final String BIRTH_DATE_SEPARATOR = "/";
    private final int GALLERY_REQUEST = 1;

    private ScrollView svForm;
    private ProgressBar pBar;

    private CircleImageView ivProfile;
    private EditText etUsername;
    private EditText etName;
    private EditText etPhone;
    private Spinner spinnerGender;
    private DatePicker dpBirth;

    private Bitmap newImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_user);

        svForm = findViewById(R.id.svForm);
        pBar = findViewById(R.id.pBar);

        ivProfile = findViewById(R.id.ivProfile);
        ivProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                askForImage();
            }
        });
        etUsername = findViewById(R.id.etUsername);
        etName = findViewById(R.id.etName);
        etPhone = findViewById(R.id.etPhone);
        spinnerGender = findViewById(R.id.spinnerGender);
        dpBirth = findViewById(R.id.dpBirth);
        Button btnSave = findViewById(R.id.btnSave);
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                save();
            }
        });

        // Set up spinner.
        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.
                createFromResource(this, R.array.genderArray, android.R.layout.simple_spinner_item);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGender.setAdapter(spinnerAdapter);

        populateUI();
        ConstraintLayout back = findViewById(R.id.backL);

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK)
            switch (requestCode) {
                case GALLERY_REQUEST:
                    Uri selectedImage = data.getData();
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImage);
                        newImage = bitmap;
                        ivProfile.setImageBitmap(bitmap);
                    } catch (IOException e) {
                        Log.i("GalleryRequest", e.getMessage());
                    }
                    break;
            }
    }

    private void save() {
        etUsername.setError(null);
        etName.setError(null);
        etPhone.setError(null);
        switchView(true);

        HashMap<UserAttributesEnum, String> attributesToUpdate = new HashMap<>();

        String username = etUsername.getText().toString().trim(),
                name = etName.getText().toString().trim(),
                phone = etPhone.getText().toString().trim(),
                gender = spinnerGender.getSelectedItem().toString(),
                birth = getFormattedDate(dpBirth);


        if (!TextUtils.isEmpty(username))
            attributesToUpdate.put(UserAttributesEnum.USERNAME, username);

        if (!TextUtils.isEmpty(name))
            attributesToUpdate.put(UserAttributesEnum.NAME, name);

        if (AttributesValidator.isPhoneValid(phone))
            attributesToUpdate.put(UserAttributesEnum.PHONE, phone);
        else if (!TextUtils.isEmpty(phone)) {
            etPhone.requestFocus();
            etPhone.setError(getApplicationContext().getResources().getString(R.string.invalid));
            return;
        }

        attributesToUpdate.put(UserAttributesEnum.GENDER, gender);
        attributesToUpdate.put(UserAttributesEnum.BIRTH_DATE, birth);

        LowKeyApplication.userManager.updateUserAttributes(attributesToUpdate, this, new AuthCallback() {
            @Override
            public void execute() {
                if (newImage != null) {
                    new ProfilePhotoUploader(newImage).upload(
                            LowKeyApplication.userManager.getUser().getUserId(),
                            new Callback() {
                                @Override
                                public void handle() {
                                    LowKeyApplication.profilePhoto = newImage;
                                    onSuccessLogic();
                                }
                            },
                            new Callback() {
                                @Override
                                public void handle() {
                                    Toast.makeText(EditUserActivity.this,
                                            EditUserActivity.this.getResources().getString(R.string.edit_fail_message),
                                            Toast.LENGTH_SHORT).show();
                                    switchView(false);
                                }
                            }
                    );
                } else {
                    onSuccessLogic();
                }
            }
        }, new AuthCallback() {
            @Override
            public void execute() {
                switchView(false);
            }
        });
    }

    private void onSuccessLogic() {
        Toast.makeText(EditUserActivity.this,
                EditUserActivity.this.getResources().getString(R.string.edit_success_message),
                Toast.LENGTH_SHORT).show();

        LowKeyApplication.userManager.requestUserDetails(EditUserActivity.this, null);

        Intent intent = new Intent(EditUserActivity.this, Main2Activity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);

        // Reload the user details locally.

        //LowKeyApplication.userManager.requestUserDetails(EditUserActivity.this, null); -> linia asta nu se mai executa dupa ce
        //dai intentul

    }

    private void populateUI() {
        switchView(true);
        try {
            Map<String, String> attributes = LowKeyApplication.userManager.getUserDetails().getAttributes().getAttributes();

            String username = attributes.get(UserAttributesEnum.USERNAME.toString()),
                    name = attributes.get(UserAttributesEnum.NAME.toString()),
                    phone = attributes.get(UserAttributesEnum.PHONE.toString()),
                    gender = attributes.get(UserAttributesEnum.GENDER.toString()),
                    birth = attributes.get(UserAttributesEnum.BIRTH_DATE.toString());

            // Get spinner default position.
            ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.
                    createFromResource(this, R.array.genderArray, android.R.layout.simple_spinner_item);
            int gender_spinner_pos = spinnerAdapter.getPosition(gender != null ? gender : spinnerAdapter.getItem(0));

            etUsername.setText(username != null ? username : "");
            etName.setText(name != null ? name : "");
            etPhone.setText(phone != null ? phone : "");
            spinnerGender.setSelection(gender_spinner_pos, true);

            if (birth != null) {
                try {
                    String[] date = birth.split(BIRTH_DATE_SEPARATOR);
                    dpBirth.init(Integer.parseInt(date[2]),
                            Integer.parseInt(date[1]),
                            Integer.parseInt(date[0]),
                            null);
                } catch (Exception e) {
                    Log.e("Birth date", e.getMessage());
                }
            }

            // Try to get profile photo from S3.
            if(LowKeyApplication.profilePhoto != null) {
                ivProfile.setImageBitmap(LowKeyApplication.profilePhoto);
                switchView(false);
            }
            else {
                final ProfilePhotoUploader profilePhotoUploader = new ProfilePhotoUploader();
                profilePhotoUploader.download(
                        LowKeyApplication.userManager.getUser().getUserId(),
                        new Callback() {
                            @Override
                            public void handle() {
                                ivProfile.setImageBitmap(profilePhotoUploader.getPhoto());
                                switchView(false);
                            }
                        },
                        new Callback() {
                            @Override
                            public void handle() {
                                ivProfile.setBackgroundResource(R.drawable.avatar_placeholder);
                                switchView(false);
                            }
                        }
                );
            }

        } catch (NullPointerException e) {
            Log.e("NullPointerExp", "User details not loaded yet");
            switchView(false);
        }
    }

    private String getFormattedDate(@NonNull DatePicker datePicker) {
        int day = datePicker.getDayOfMonth();
        int month = datePicker.getMonth();
        int year = datePicker.getYear();

        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, day);

        SimpleDateFormat sdf = new SimpleDateFormat("dd" + BIRTH_DATE_SEPARATOR + "MM" + BIRTH_DATE_SEPARATOR + "yyyy");
        return sdf.format(calendar.getTime());
    }

    private void askForImage() {
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        startActivityForResult(photoPickerIntent, GALLERY_REQUEST);
    }

    private void switchView(boolean loading) {
        pBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        svForm.setVisibility(loading ? View.GONE : View.VISIBLE);
    }

}