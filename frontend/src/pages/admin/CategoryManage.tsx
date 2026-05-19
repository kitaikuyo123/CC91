import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  getCategories, createCategory, updateCategory, deleteCategory,
  type Category, type CreateCategoryRequest, type UpdateCategoryRequest
} from '../../api/category';
import ErrorMessage from '../../components/ErrorMessage';
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
        return updateCategory(id, data as UpdateCategoryRequest);
      } else {
        return createCategory(data);
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
    mutationFn: deleteCategory,
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

  if (isLoading) {
    return (
      <div className="loading-container">
        <div className="spinner spinner-lg"></div>
        <span>加载中...</span>
      </div>
    );
  }

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
        <h1>版块管理</h1>
        <button className="btn btn-primary" onClick={handleCreate}>
          + 新建版块
        </button>
      </div>

      {error && <ErrorMessage message={error} onDismiss={() => setError('')} />}
      {success && <ErrorMessage type="success" message={success} onDismiss={() => setSuccess('')} />}

      {showForm && (
        <div className="card" style={{ marginBottom: '1.5rem', padding: '1.5rem' }}>
          <h2 style={{ marginBottom: '1rem' }}>{editingId ? '编辑版块' : '新建版块'}</h2>
          <form onSubmit={handleSubmit}>
            <div className="form-group">
              <label htmlFor="cat-name">版块名称 <span aria-hidden="true">*</span></label>
              <input
                id="cat-name"
                type="text"
                value={formData.name}
                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                placeholder="请输入版块名称"
                required
              />
            </div>

            <div className="form-group">
              <label htmlFor="cat-desc">版块描述</label>
              <textarea
                id="cat-desc"
                value={formData.description}
                onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                placeholder="请输入版块描述"
                rows={3}
              />
            </div>

            <div className="form-group">
              <label htmlFor="cat-sort">排序顺序</label>
              <input
                id="cat-sort"
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
        <div className="table-container">
          <table className="data-table">
            <thead>
              <tr>
                <th>排序</th>
                <th>名称</th>
                <th>描述</th>
                <th>操作</th>
            </tr>
          </thead>
          <tbody>
            {categories.length === 0 ? (
              <tr>
                <td colSpan={4} className="empty-state">
                  暂无版块
                </td>
              </tr>
            ) : (
              categories.map((category) => (
                <tr key={category.id}>
                  <td>{category.sortOrder}</td>
                  <td style={{ fontWeight: '500' }}>{category.name}</td>
                  <td className="hide-mobile">{category.description || '-'}</td>
                  <td>
                    <button
                      className="btn btn-sm"
                      onClick={() => handleEdit(category)}
                      style={{ marginRight: '0.5rem' }}
                    >
                      编辑
                    </button>
                    <button
                      className="btn btn-danger btn-sm"
                      onClick={() => handleDelete(category.id, category.name)}
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
    </div>
  );
}
