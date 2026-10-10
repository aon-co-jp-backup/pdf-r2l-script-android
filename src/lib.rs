//! PDFを右綴じ(R2L)かつ見開き右始まり(TwoColumnRight)にするコア。AndroidからはJNI経由(`Native.convert`)で呼ぶ。
use jni::objects::{JByteArray, JObject};
use jni::sys::jint;
use jni::JNIEnv;
use lopdf::{Dictionary, Document, Object};

/// 元スクリプト(pdfr2l.py)と同じく Direction=R2L・PageLayout=TwoColumnRight にした PDF のバイト列を返す。
/// 既存の ViewerPreferences の他の項目は保持する(元スクリプトは置き換え)。
pub fn convert_bytes(src: &[u8]) -> Result<Vec<u8>, String> {
    let mut doc = Document::load_mem(src).map_err(|e| format!("読み込み失敗: {e}"))?;
    let root_id = doc
        .trailer
        .get(b"Root")
        .and_then(|o| o.as_reference())
        .map_err(|e| format!("Catalogが見つかりません: {e}"))?;

    let vp_ref = doc
        .get_dictionary(root_id)
        .ok()
        .and_then(|c| c.get(b"ViewerPreferences").ok())
        .and_then(|o| o.as_reference().ok());
    let dir = Object::Name(b"R2L".to_vec());
    if let Some(id) = vp_ref {
        doc.get_dictionary_mut(id)
            .map_err(|e| e.to_string())?
            .set("Direction", dir.clone());
    }
    let catalog = doc.get_dictionary_mut(root_id).map_err(|e| e.to_string())?;
    if vp_ref.is_none() {
        match catalog.get_mut(b"ViewerPreferences") {
            Ok(Object::Dictionary(d)) => d.set("Direction", dir),
            _ => {
                let mut d = Dictionary::new();
                d.set("Direction", dir);
                catalog.set("ViewerPreferences", Object::Dictionary(d));
            }
        }
    }
    catalog.set("PageLayout", Object::Name(b"TwoColumnRight".to_vec()));
    let mut out = Vec::new();
    doc.save_to(&mut out).map_err(|e| format!("保存失敗: {e}"))?;
    Ok(out)
}

/// Kotlin: `external fun convert(input: ByteArray, mode: Int): ByteArray`。
/// `mode` は未使用(r2l版と同じシグネチャにしてあるだけ)。失敗時は RuntimeException を投げる。
#[no_mangle]
pub extern "system" fn Java_tokyo_aon_pdfr2lscript_Native_convert<'l>(
    mut env: JNIEnv<'l>,
    _this: JObject<'l>,
    input: JByteArray<'l>,
    _mode: jint,
) -> JByteArray<'l> {
    let null = || unsafe { JByteArray::from_raw(std::ptr::null_mut()) };
    let bytes = match env.convert_byte_array(&input) {
        Ok(b) => b,
        Err(e) => {
            let _ = env.throw_new("java/lang/RuntimeException", e.to_string());
            return null();
        }
    };
    match convert_bytes(&bytes) {
        Ok(out) => env.byte_array_from_slice(&out).unwrap_or_else(|_| null()),
        Err(msg) => {
            let _ = env.throw_new("java/lang/RuntimeException", msg);
            null()
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use lopdf::dictionary;

    fn sample() -> Vec<u8> {
        let mut doc = Document::with_version("1.5");
        let pages = doc.new_object_id();
        let page = doc.add_object(dictionary! {"Type"=>"Page","Parent"=>pages,"MediaBox"=>vec![0.into(),0.into(),100.into(),100.into()]});
        doc.objects.insert(pages, Object::Dictionary(dictionary! {"Type"=>"Pages","Kids"=>vec![page.into()],"Count"=>1}));
        let cat = doc.add_object(dictionary! {"Type"=>"Catalog","Pages"=>pages});
        doc.trailer.set("Root", cat);
        let mut v = Vec::new();
        doc.save_to(&mut v).unwrap();
        v
    }

    #[test]
    fn converts() {
        let b = convert_bytes(&sample()).unwrap();
        let doc = Document::load_mem(&b).unwrap();
        let root = doc.trailer.get(b"Root").unwrap().as_reference().unwrap();
        let cat = doc.get_dictionary(root).unwrap();
        let vp = cat.get(b"ViewerPreferences").unwrap().as_dict().unwrap();
        assert_eq!(vp.get(b"Direction").unwrap().as_name().unwrap(), b"R2L");
        assert_eq!(cat.get(b"PageLayout").unwrap().as_name().unwrap(), b"TwoColumnRight");
        assert!(convert_bytes(b"not a pdf").is_err());
    }
}
