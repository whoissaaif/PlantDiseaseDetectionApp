
from fastapi import FastAPI, File, UploadFile
from PIL import Image
import tensorflow as tf
import numpy as np
import io

app = FastAPI()

# Load your trained model
# Replace '''path/to/your/model.h5''' with the actual path to your model
# model = tf.keras.models.load_model('''path/to/your/model.h5''')

# Define the class names
# Make sure this order matches the output of your model
class_names = ['''Apple___Apple_scab''', '''Apple___Black_rot''', '''Apple___Cedar_apple_rust''', '''Apple___healthy''',
               '''Corn_(maize)___Cercospora_leaf_spot Gray_leaf_spot''', '''Corn_(maize)___Common_rust_''',
               '''Corn_(maize)___Northern_Leaf_Blight''', '''Corn_(maize)___healthy''', '''Grape___Black_rot''',
               '''Grape___Esca_(Black_Measles)''', '''Grape___Leaf_blight_(Isariopsis_Leaf_Spot)''',
               '''Grape___healthy''', '''Potato___Early_blight''', '''Potato___Late_blight''',
               '''Potato___healthy''', '''Tomato___Bacterial_spot''', '''Tomato___Early_blight''',
               '''Tomato___Late_blight''', '''Tomato___Leaf_Mold''', '''Tomato___Septoria_leaf_spot''',
               '''Tomato___Spider_mites Two-spotted_spider_mite''', '''Tomato___Target_Spot''',
               '''Tomato___Tomato_Yellow_Leaf_Curl_Virus''', '''Tomato___Tomato_mosaic_virus''',
               '''Tomato___healthy''']


@app.get("/")
def read_root():
    return {"Hello": "World"}


def preprocess_image(image: Image.Image):
    # Preprocess the image to match the model'''s input requirements
    # (e.g., resize, normalize)
    image = image.resize((224, 224))  # Example size, adjust as needed
    image = np.array(image) / 255.0
    image = np.expand_dims(image, axis=0)
    return image


@app.post("/api/predict")
async def predict(file: UploadFile = File(...)):
    # For a real implementation, you would load the model once at startup
    # For this example, we'''ll just return a mock response
    # To use the actual model, uncomment the following lines
    # content = await file.read()
    # image = Image.open(io.BytesIO(content))
    # processed_image = preprocess_image(image)
    # prediction = model.predict(processed_image)
    # predicted_class = class_names[np.argmax(prediction)]
    # confidence = np.max(prediction)

    # Mock response for demonstration
    predicted_class = "Tomato___Tomato_Yellow_Leaf_Curl_Virus"
    confidence = 0.95

    return {"predicted_disease": predicted_class, "confidence_score": confidence}

