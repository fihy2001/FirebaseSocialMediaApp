package com.example.firebasesocialmediaapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.dynamic.IFragmentWrapper;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class SocialMediaActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    private FirebaseAuth mAuth;
    private ImageView postImageView;
    private EditText edtDescription;
    private Button btnCreatePost;
    private ListView usersListView;
    private Bitmap bitmap;
    private String imageIdentifier;

    private ArrayList<String> usernames;
    private ArrayAdapter adapter;

    private ArrayList<String> uids;
    private String imageDownloadLink;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_social_media);

        mAuth = FirebaseAuth.getInstance();

        postImageView = findViewById(R.id.postImageView);
        edtDescription = findViewById(R.id.edtDescription);
        btnCreatePost = findViewById(R.id.btnCreatePost);
        usersListView = findViewById(R.id.usersListView);
        usernames = new ArrayList<>();
        adapter = new ArrayAdapter(SocialMediaActivity.this, android.R.layout.simple_list_item_1, usernames);
        uids = new ArrayList<>();

        usersListView.setAdapter(adapter);

        usersListView.setOnItemClickListener(this);

        btnCreatePost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                uploadImageToServer();

            }
        });

        postImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                selectImage();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_logout, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){

            case R.id.logoutUserItem:

                logout();

                break;
            case R.id.viewPostItem:

                Intent intent = new Intent(SocialMediaActivity.this, ViewPostsActivity.class);
                startActivity(intent);

                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {

        logout();
        super.onBackPressed();
    }
    private void logout (){
        mAuth.signOut();
        finish();
    }
    private void selectImage() {
        if (Build.VERSION.SDK_INT < 23) {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, 1000);
        } else if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1000);
            } else {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, 1000);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1000 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            selectImage();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1000 && resultCode == RESULT_OK &&  data != null){
            Uri chosenImageData = data.getData();
            try {
                bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), chosenImageData);
                postImageView.setImageBitmap(bitmap);

            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }
    private void uploadImageToServer(){

        if (bitmap != null) {
            // Get the data from an ImageView as bytes
            postImageView.setDrawingCacheEnabled(true);
            postImageView.buildDrawingCache();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
            byte[] data = baos.toByteArray();

            imageIdentifier = UUID.randomUUID() + ".png";

            UploadTask uploadTask = FirebaseStorage.getInstance().getReference().child("my_images").child(imageIdentifier).putBytes(data);
            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    // Handle unsuccessful uploads
                    Toast.makeText(SocialMediaActivity.this, exception.toString(), Toast.LENGTH_LONG).show();
                }
            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    // taskSnapshot.getMetadata() contains file metadata such as size, content-type, etc.
                    // ...
                    Toast.makeText(SocialMediaActivity.this, "Upload Successful", Toast.LENGTH_LONG).show();

                    edtDescription.setVisibility(View.VISIBLE);

                    FirebaseDatabase.getInstance().getReference().child("my_users").addChildEventListener(new ChildEventListener() {
                        @Override
                        public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String previousChildName) {

                            uids.add(dataSnapshot.getKey());
                            String username = (String) dataSnapshot.child("username").getValue();
                            usernames.add(username);
                            adapter.notifyDataSetChanged();
                        }

                        @Override
                        public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                        }

                        @Override
                        public void onChildRemoved(@NonNull DataSnapshot snapshot) {

                        }

                        @Override
                        public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });

                    taskSnapshot.getMetadata().getReference().getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                        @Override
                        public void onComplete(@NonNull Task<Uri> task) {
                            if (task.isSuccessful()){
                                imageDownloadLink = task.getResult().toString();
                            }
                        }
                    });
                }
            });
        }

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        if (FirebaseAuth.getInstance().getCurrentUser().getUid().equals(uids.get(position))) {
            Toast.makeText(SocialMediaActivity.this, "Unable to choose self", Toast.LENGTH_LONG).show();
        } else {

            HashMap<String, String> dataMap = new HashMap<>();
            dataMap.put("fromWhom", FirebaseAuth.getInstance().getCurrentUser().getDisplayName());
            dataMap.put("imageIdentifier", imageIdentifier);
            dataMap.put("imageLink", imageDownloadLink);
            dataMap.put("des", edtDescription.getText().toString());
            FirebaseDatabase.getInstance().getReference().child("my_users").child(uids.get(position)).child("received_posts")
                    .push().setValue(dataMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()){
                        Toast.makeText(SocialMediaActivity.this, "Data Sent", Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
    }
}