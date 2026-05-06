import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getCategories, type Category } from '../../api/category';
import { adminCreateCategory, adminUpdateCategory, adminDeleteCategory } from '../../api/admin';
import type { CreateCategoryRequest, UpdateCategoryRequest } from '../../api/category';
import { queryKeys } from '../../lib/queryKeys';

/**
 * 版块管理页面
 */
export default function CategoryManage() {
  const queryClient = useQueryClient();

  const [showForm, setShowForm] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [formData, setFormData] = useState<CreateCategoryRequest>({
    name: '',
    description: '',
    sortOrder: 0
  });
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  // 使用 React Query 获取版块列表
  const { data: categories = [], isLoading } = useQuery({
    queryKey: queryKeys.categories.list(),
    queryFn: getCategories,
  });

  const handleCreate = () => {
    setEditingId(null);
    setFormData({ name: '', description: '', sortOrder: categories.length });
    setShowForm(true);
    setError('');
    setSuccess('');
  };

  const handleEdit = (category: Category) => {
    setEditingId(category.id);
    setFormData({
      name: category.name,
      description: category.description || '',
      sortOrder: category.sortOrder
    });
    setShowForm(true);
    setError('');
    setSuccess('');
  };

  // 创建/更新版块的 mutation
  const saveMutation = useMutation({
    mutationFn: ({ id, data }: { id?: number; data: CreateCategoryRequest | UpdateCategoryRequest }) => {
      if (id) {
        return adminUpdateCategory(id, data as UpdateCategoryRequest);
      } else {
        return adminCreateCategory(data);
      }
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.categories.list() });
      setShowForm(false);
      setSuccess(editingId ? '版块更新成功' : '版块创建成功');
      setEditingId(null);
    },
    onError: (err: any) => {
      setError(err.response?.data?.message || '操作失败');
    },
  });

  // 删除版块的 mutation
  const deleteMutation = useMutation({
    mutationFn: adminDeleteCategory,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.categories.list() });
      setSuccess('版块删除成功');
    },
    onError: (err: any) => {
      setError(err.response?.data?.message || '删除失败');
    },
  });

  const handleDelete = async (id: number, name: string) => {
    if (!confirm(`确定要删除版块「${name}」吗？`)) return;
    deleteMutation.mutate(id);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    if (!formData.name.trim()) {
      setError('版块名称不能为空');
      return;
    }

    saveMutation.mutate({ id: editingId || undefined, data: formData });
  };

  if (loading) {
    return <div>加载中...</div>;
  }

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
        <h1>版块管理</h1>
        <button className="btn btn-primary" onClick={handleCreate}>
          + 新建版块
        </button>
      </div>

      {error && (
        <div className="error-message" style={{ marginBottom: '1rem' }}>
          {error}
        </div>
      )}

      {success && (
        <div style={{ marginBottom: '1rem', padding: '0.75rem', background: '#d4edda', color: '#155724', borderRadius: '4px' }}>
          {success}
        </div>
      )}

      {showForm && (
        <div className="card" style={{ marginBottom: '1.5rem', padding: '1.5rem' }}>
          <h2 style={{ marginBottom: '1rem' }}>{editingId ? '编辑版块' : '新建版块'}</h2>
          <form onSubmit={handleSubmit}>
            <div className="form-group">
              <label>版块名称 *</label>
              <input
                type="text"
                value={formData.name}
                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                placeholder="请输入版块名称"
                required
              />
            </div>

            <div className="form-group">
              <label>版块描述</label>
              <textarea
                value={formData.description}
                onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                placeholder="请输入版块描述"
                rows={3}
                style={{ width: '100%', padding: '0.5rem', border: '1px solid #ddd', borderRadius: '4px' }}
              />
            </div>

            <div className="form-group">
              <label>排序顺序</label>
              <input
                type="number"
                value={formData.sortOrder}
                onChange={(e) => setFormData({ ...formData, sortOrder: parseInt(e.target.value) || 0 })}
                min="0"
              />
              <small style={{ color: '#666' }}>数字越小越靠前</small>
            </div>

            <div style={{ display: 'flex', gap: '0.5rem' }}>
              <button type="submit" className="btn btn-primary" disabled={saveMutation.isPending}>
                {saveMutation.isPending ? '提交中...' : (editingId ? '保存' : '创建')}
              </button>
              <button type="button" className="btn" onClick={() => setShowForm(false)}>
                取消
              </button>
            </div>
          </form>
        </div>
      )}

      <div className="card">
        <table style={{ width: '100%', borderCollapse: 'collapse' }}>
          <thead>
            <tr style={{ borderBottom: '2px solid #eee' }}>
              <th style={{ padding: '0.75rem', textAlign: 'left' }}>排序</th>
              <th style={{ padding: '0.75rem', textAlign: 'left' }}>名称</th>
              <th style={{ padding: '0.75rem', textAlign: 'left' }}>描述</th>
              <th style={{ padding: '0.75rem', textAlign: 'left' }}>操作</th>
            </tr>
          </thead>
          <tbody>
            {categories.length === 0 ? (
              <tr>
                <td colSpan={4} style={{ padding: '2rem', textAlign: 'center', color: '#999' }}>
                  暂无版块
                </td>
              </tr>
            ) : (
              categories.map((category) => (
                <tr key={category.id} style={{ borderBottom: '1px solid #eee' }}>
                  <td style={{ padding: '0.75rem' }}>{category.sortOrder}</td>
                  <td style={{ padding: '0.75rem', fontWeight: '500' }}>{category.name}</td>
                  <td style={{ padding: '0.75rem', color: '#666' }}>
                    {category.description || '-'}
                  </td>
                  <td style={{ padding: '0.75rem' }}>
                    <button
                      className="btn"
                      onClick={() => handleEdit(category)}
                      style={{ marginRight: '0.5rem', fontSize: '0.85rem' }}
                    >
                      编辑
                    </button>
                    <button
                      className="btn"
                      onClick={() => handleDelete(category.id, category.name)}
                      style={{ background: '#e74c3c', color: '#fff', fontSize: '0.85rem' }}
                    >
                      删除
                    </button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
