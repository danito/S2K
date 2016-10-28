package be.nixekinder.ShareWithKnown;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import be.nixekinder.preferencestest.R;

public class shareImageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_image);
    }
    /**
     *
     *
     ivRemoveImage = (ImageView) findViewById(R.id.remove_image);
     ivRemoveImage.setOnClickListener(new View.OnClickListener() {
    @Override public void onClick(View v) {
    int visibility = ivRemoveImage.getVisibility();
    if (visibility == View.VISIBLE ){
    removeImage();
    }
    }
    });

     ivSetImage = (ImageView)findViewById(R.id.add_image);
     ivSetImage.setOnClickListener(new View.OnClickListener() {
    @Override public void onClick(View v) {
    setImage();
    }
    });



     private void removeImage() {
     image = null;
     ivShareImage.setImageResource(0);

     }

     private void setImage() {
     Intent intent = new Intent();
     // Show only images, no videos or anything else
     intent.setType("image/*");
     intent.setAction(Intent.ACTION_GET_CONTENT);
     // Always show the chooser (if there are multiple options available)
     startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
     }
     @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
     super.onActivityResult(requestCode, resultCode, data);

     if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {

     Uri uri = data.getData();

     try {
     Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
     // Log.d(TAG, String.valueOf(bitmap));


     ivShareImage = (ImageView) findViewById(R.id.shareImage);
     //  imageView.setImageBitmap(bitmap);
     Picasso.with(getBaseContext()).load(uri).fit().into(ivShareImage);
     image = uri;
     ivRemoveImage.setVisibility(View.VISIBLE);

     } catch (IOException e) {
     e.printStackTrace();
     }
     }
     if (requestCode == 0){
     showPrefs();
     }
     }
     /*
     @Override protected void onActivityResult(int requestCode, int resultCode, Intent data){
     // check what happened with settings
     showPrefs();
     }


     */


}
