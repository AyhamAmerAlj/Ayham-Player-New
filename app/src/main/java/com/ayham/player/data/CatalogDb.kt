package com.ayham.player.data

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.database.sqlite.SQLiteStatement
import com.ayham.player.ui.normalizeForSearch

/**
 * قاعدة بيانات القائمة على الجهاز.
 * بدل ما نخزّن عشرات الآلاف من العناصر بالذاكرة (وتختنق الشاشات الضعيفة)،
 * نخزّنها هنا ونقرأ فقط الصفوف الظاهرة على الشاشة.
 */
class CatalogDb(ctx: Context) : SQLiteOpenHelper(ctx.applicationContext, "catalog.db", null, 1) {

    init {
        setWriteAheadLoggingEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE items(_id INTEGER PRIMARY KEY AUTOINCREMENT, kind INTEGER NOT NULL, sid TEXT NOT NULL," +
                " name TEXT, norm TEXT, image TEXT, cat TEXT, rating REAL, ext TEXT, added INTEGER, UNIQUE(kind, sid))",
        )
        db.execSQL("CREATE INDEX i_cat ON items(kind, cat)")
        db.execSQL("CREATE INDEX i_added ON items(kind, added)")
        db.execSQL("CREATE INDEX i_rating ON items(kind, rating)")
        db.execSQL("CREATE TABLE cats(kind INTEGER NOT NULL, cid TEXT NOT NULL, name TEXT, cnt INTEGER DEFAULT 0, PRIMARY KEY(kind, cid))")
        db.execSQL("CREATE TABLE meta(k TEXT PRIMARY KEY, v TEXT)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    // ---------------- meta ----------------
    fun meta(k: String): String? =
        readableDatabase.rawQuery("SELECT v FROM meta WHERE k=?", arrayOf(k)).use { if (it.moveToFirst()) it.getString(0) else null }

    fun setMeta(k: String, v: String) {
        writableDatabase.execSQL("INSERT OR REPLACE INTO meta(k, v) VALUES(?, ?)", arrayOf(k, v))
    }

    fun clearAll() {
        val db = writableDatabase
        db.execSQL("DELETE FROM items")
        db.execSQL("DELETE FROM cats")
    }

    fun clearKind(kind: Kind) {
        val db = writableDatabase
        db.execSQL("DELETE FROM items WHERE kind=?", arrayOf<Any>(kind.ordinal))
        db.execSQL("DELETE FROM cats WHERE kind=?", arrayOf<Any>(kind.ordinal))
    }

    fun count(kind: Kind): Int =
        readableDatabase.rawQuery("SELECT COUNT(*) FROM items WHERE kind=?", arrayOf(kind.ordinal.toString()))
            .use { if (it.moveToFirst()) it.getInt(0) else 0 }

    // ---------------- الكتابة (دفعات) ----------------
    class Writer(private val db: SQLiteDatabase, private val kind: Kind) {
        private val stmt: SQLiteStatement = db.compileStatement(
            "INSERT OR IGNORE INTO items(kind,sid,name,norm,image,cat,rating,ext,added) VALUES(?,?,?,?,?,?,?,?,?)",
        )
        private var n = 0

        init {
            db.beginTransaction()
        }

        fun add(sid: String, name: String, image: String, cat: String, rating: Double, ext: String, added: Long) {
            stmt.clearBindings()
            stmt.bindLong(1, kind.ordinal.toLong())
            stmt.bindString(2, sid)
            stmt.bindString(3, name)
            stmt.bindString(4, normalizeForSearch(name))
            stmt.bindString(5, image)
            stmt.bindString(6, cat)
            stmt.bindDouble(7, rating)
            stmt.bindString(8, ext)
            stmt.bindLong(9, added)
            stmt.executeInsert()
            if (++n % 2000 == 0) {
                db.setTransactionSuccessful()
                db.endTransaction()
                db.beginTransaction()
            }
        }

        fun commit() {
            db.setTransactionSuccessful()
            db.endTransaction()
            stmt.close()
        }

        fun abort() {
            runCatching { db.endTransaction() }
            runCatching { stmt.close() }
        }
    }

    fun writer(kind: Kind) = Writer(writableDatabase, kind)

    fun replaceCats(kind: Kind, cats: List<Category>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.execSQL("DELETE FROM cats WHERE kind=?", arrayOf<Any>(kind.ordinal))
            for (c in cats) {
                db.execSQL("INSERT OR REPLACE INTO cats(kind,cid,name) VALUES(?,?,?)", arrayOf<Any>(kind.ordinal, c.id, c.name))
            }
            db.execSQL(
                "UPDATE cats SET cnt=(SELECT COUNT(*) FROM items i WHERE i.kind=cats.kind AND i.cat=cats.cid) WHERE kind=?",
                arrayOf<Any>(kind.ordinal),
            )
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    // ---------------- القراءة ----------------
    fun categories(kind: Kind): List<Category> {
        val out = ArrayList<Category>()
        readableDatabase.rawQuery(
            "SELECT cid, name, cnt FROM cats WHERE kind=? AND cnt>0 ORDER BY rowid",
            arrayOf(kind.ordinal.toString()),
        ).use { c -> while (c.moveToNext()) out.add(Category(c.getString(0) ?: "", c.getString(1) ?: "", c.getInt(2))) }
        return out
    }

    private val cols = "kind,sid,name,image,cat,rating,ext,added,_id"

    private fun row(c: Cursor): Item {
        val r = c.getDouble(5)
        return Item(
            kind = Kind.values()[c.getInt(0)],
            id = c.getString(1) ?: "",
            name = c.getString(2) ?: "",
            image = c.getString(3) ?: "",
            categoryId = c.getString(4) ?: "",
            rating = if (r > 0) String.format(java.util.Locale.US, "%.1f", r) else "",
            ext = c.getString(6) ?: "",
            added = c.getLong(7),
            rid = c.getLong(8),
        )
    }

    private fun query(kind: Kind, where: String, args: List<String>, order: String, limit: Int, offset: Int = 0): List<Item> {
        val sql = "SELECT $cols FROM items WHERE kind=?" + (if (where.isEmpty()) "" else " AND ($where)") +
            " ORDER BY $order LIMIT $limit OFFSET $offset"
        val out = ArrayList<Item>(minOf(limit, 256))
        readableDatabase.rawQuery(sql, (listOf(kind.ordinal.toString()) + args).toTypedArray()).use { c ->
            while (c.moveToNext()) out.add(row(c))
        }
        return out
    }

    fun recent(kind: Kind, n: Int) = query(kind, "added>0", emptyList(), "added DESC", n)
    fun top(kind: Kind, n: Int) = query(kind, "rating>0", emptyList(), "rating DESC", n)
    fun all(kind: Kind, limit: Int, offset: Int) = query(kind, "", emptyList(), "_id", limit, offset)
    fun inCategory(kind: Kind, cid: String, limit: Int, offset: Int) = query(kind, "cat=?", listOf(cid), "_id", limit, offset)

    /** عناصر مشابهة: نفس السلسلة (كلمة مشتركة بالاسم مثل 007) ثم نفس التصنيف بترتيب التقييم. */
    fun similar(item: Item, limit: Int): List<Item> {
        val out = LinkedHashMap<String, Item>()
        fun add(list: List<Item>) {
            for (i in list) if (i.id != item.id && out.size < limit) out.putIfAbsent(i.id, i)
        }
        val stop = setOf("the", "and", "part", "vol", "movie", "film", "season", "episode", "with", "for", "from", "into")
        val cleaned = item.name.replace(Regex("\\[[^\\]]*\\]|\\([^)]*\\)"), " ")
        val cands = ArrayList<String>()
        // أجزاء مفصولة بشرطة: الجزء القصير غالبًا اسم السلسلة
        val parts = cleaned.split(Regex("\\s[-–:|]\\s")).map { com.ayham.player.ui.normalizeForSearch(it) }.filter { it.length >= 3 }
        for (p in parts.reversed()) if (p.split(" ").size <= 2) cands.add(p)
        for (w in com.ayham.player.ui.normalizeForSearch(cleaned).split(Regex("\\s+"))) {
            if (w.length >= 3 && w !in stop && !Regex("(19|20)\\d{2}").matches(w)) cands.add(w)
        }
        for (c in cands.distinct().take(4)) {
            val q = c.replace("%", "").replace("_", "")
            val r = query(item.kind, "norm LIKE ?", listOf("%$q%"), "rating DESC", 40).filter { it.id != item.id }
            if (r.size >= 3) { add(r); break }
        }
        if (item.categoryId.isNotEmpty()) add(query(item.kind, "cat=?", listOf(item.categoryId), "rating DESC", limit + 1))
        return out.values.toList()
    }

    fun search(kind: Kind, qNorm: String, limit: Int): List<Item> {
        val q = qNorm.replace("%", "").replace("_", "")
        if (q.isEmpty()) return emptyList()
        return query(kind, "norm LIKE ?", listOf("%$q%"), "_id", limit)
    }

    fun byId(kind: Kind, sid: String): Item? = query(kind, "sid=?", listOf(sid), "_id", 1).firstOrNull()

    /** القنوات المحيطة بقناة معيّنة (لتبديل القنوات بسرعة). */
    fun around(kind: Kind, cat: String?, rid: Long, before: Int, after: Int): Pair<List<Item>, Int> {
        val catClause = if (cat == null) "" else " AND cat=?"
        val extra = if (cat == null) emptyList() else listOf(cat)
        val fwd = query(kind, "_id>=?$catClause", listOf(rid.toString()) + extra, "_id", after)
        val back = query(kind, "_id<?$catClause", listOf(rid.toString()) + extra, "_id DESC", before).reversed()
        return (back + fwd) to back.size
    }
}
