package com.yinyue.repository;

import com.yinyue.entity.MusicTrack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * ======================================================================================
 * 类名：MusicTrackRepository (音乐仓库管理员)
 * 
 * 作用：这个接口专门负责跟数据库打交道。
 * 
 * 我们不需要自己写 SQL 语句（比如 "SELECT * FROM music_track"），
 * 因为 Spring Data JPA 会自动帮我们把这些活干了。
 * 
 * 只要继承了 JpaRepository，我们就能直接使用 save(保存), findById(查找), delete(删除) 等方法。
 * 这就像是给了我们一个万能遥控器，按一下按钮就能操作数据库。
 * ======================================================================================
 */
@Repository // 告诉 Spring：这是一个负责数据存取的组件。
public interface MusicTrackRepository extends JpaRepository<MusicTrack, Long> {

    /**
     * 方法名：findByStatusOrderByCreatedAtDesc (根据状态查找，并按时间倒序排列)
     * 
     * 作用：这是一个“魔法方法”。
     * 
     * 你看这个方法名字很长，其实它是遵循特定规则的咒语：
     * findBy (查找) + Status (状态字段) + OrderBy (排序) + CreatedAt (创建时间) + Desc (倒序/最新的在前)。
     * 
     * 当我们调用这个方法并传入 "COMPLETED" 时，
     * Spring Data JPA 会自动把它翻译成类似这样的 SQL：
     * "SELECT * FROM music_track WHERE status = 'COMPLETED' ORDER BY created_at DESC"
     * 
     * 这样我们就能很方便地拿到所有已经处理完的歌曲，而且最新的歌排在最前面。
     */
    List<MusicTrack> findByStatusOrderByCreatedAtDesc(String status);
}
